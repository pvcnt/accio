/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.lumos.storage.mysql

import com.twitter.finagle.mysql.Parameter.{NullParameter, wrap}
import com.twitter.finagle.mysql._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Future, StorageUnit, Time}
import fr.cnrs.liris.lumos.domain._
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.storage.{JobQuery, JobStore}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.mutable

private[storage] final class MysqlJobStore(client: Client, statsReceiver: StatsReceiver)
  extends JobStore {

  private[this] val sizeStat = statsReceiver.stat("storage", "job", "content_size_kb")

  override def create(job: Job): Future[Status] = {
    val content = encode(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare(MysqlJobStore.InsertQuery)
      .apply(
        job.name,
        job.createTime.getMillis,
        job.status.state.name,
        job.owner.map(wrap(_)).getOrElse(NullParameter),
        content)
      .flatMap {
        case _: OK =>
          val fs = job.labels.toSeq.map { case (k, v) =>
            client.prepare(MysqlJobStore.InsertLabelQuery).apply(job.name, k, v)
          }
          Future.join(fs).map(_ => Status.Ok)
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
      .handle {
        // Error code 1062 corresponds to a duplicate entry, which means the object already exists.
        case ServerError(1062, _, _) => Status.AlreadyExists(job.name)
      }
  }

  override def replace(job: Job): Future[Status] = {
    // Create time, owner and labels are all immutable, so we do not update their denormalizations.
    // Only the state might be updated later on.
    val content = encode(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare(MysqlJobStore.ReplaceQuery)
      .apply(job.status.state.name, content, job.name)
      .map {
        case ok: OK =>
          if (ok.affectedRows == 1) Status.Ok else Status.NotFound(job.name)
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
  }

  override def delete(name: String): Future[Status] = {
    client
      .prepare(MysqlJobStore.DeleteQuery)
      .apply(name)
      .map {
        case ok: OK =>
          if (ok.affectedRows == 1) Status.Ok else Status.NotFound(name)
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
  }

  override def list(query: JobQuery, limit: Option[Int], offset: Option[Int]): Future[JobList] = {
    val where = mutable.ListBuffer.empty[String]
    val params = mutable.ListBuffer.empty[Parameter]
    query.owner.foreach { owner =>
      where += "owner = ?"
      params += owner
    }
    if (query.state.nonEmpty) {
      where += s"state in ${sqlSet(query.state)}"
      query.state.foreach(v => params += v.name)
    }
    query.labels.foreach { selector =>
      selector.requirements.foreach { req =>
        req.op match {
          case LabelSelector.Absent =>
            where += "(select count(1) from jobs_labels where name = t.name and label_key = ?) = 0"
            params += req.key
          case LabelSelector.Present =>
            where += "(select count(1) from jobs_labels where name = t.name and label_key = ?) > 0"
            params += req.key
          case LabelSelector.In =>
            // We assume that the selector is valid and hence `values` is not empty (otherwise the
            // SQL clause `in()` would be problematic).
            where += "(select label_value from jobs_labels where name = t.name and label_key = ?) " +
              s"in (${Seq.fill(req.values.size)("?").mkString(", ")})"
            params += req.key
            req.values.foreach(v => params += v)
          case LabelSelector.NotIn =>
            // We prefer re-using the same sub-query twice (`getValueQuery`) instead of performing
            // a count first (as in the absent case above), because we expect MySQL to optimize
            // when the same sub-query appears twice and only execute it once.
            val getValueQuery = "(select label_value from jobs_labels where name = t.name and label_key = ?)"
            // We assume that the selector is valid and hence `values` is not empty (otherwise the
            // SQL clause `in()` would be problematic).
            where += s"($getValueQuery is null or $getValueQuery not in ${sqlSet(req.values)})"
            params += req.key
            params += req.key
            req.values.foreach(v => params += v)
        }
      }
    }

    var sql = s"select content from jobs t where ${if (where.nonEmpty) where.mkString(" and ") else "true"} order by create_time desc"
    (offset, limit) match {
      case (Some(o), Some(l)) => sql += s" limit $l offset $o"
      case (Some(o), None) => sql += s" limit ${Int.MaxValue} offset $o"
      case (None, Some(l)) => sql += s" limit $l"
      case (None, None) => // Do nothing.
    }
    val sql2 = s"select count(1) from jobs t where ${if (where.nonEmpty) where.mkString(" and ") else "true"}"

    Future.join(
      client.prepare(sql).select(params: _*)(decodeJob),
      client.prepare(sql2).select(params: _*)(decodeCount).map(_.head))
      .map { case (jobs, totalCount) => JobList(jobs, totalCount) }
  }

  override def get(name: String): Future[Option[Job]] = {
    client
      .prepare(MysqlJobStore.GetQuery)
      .select(name)(decodeJob)
      .map(_.headOption)
  }

  private def encode(job: Job): Array[Byte] = {
    val obj = ThriftAdapter.toThrift(job)
    BinaryScroogeSerializer.toBytes(obj)
  }

  private def decodeJob(row: Row): Job =
    row("content").get match {
      case raw: RawValue =>
        val obj = BinaryScroogeSerializer.fromBytes(raw.bytes, thrift.Job)
        ThriftAdapter.toDomain(obj)
      case v => throw new RuntimeException(s"Unexpected content value: $v")
    }

  private def decodeCount(row: Row): Long =
    row.values.head match {
      case IntValue(v) => v
      case LongValue(v) => v
      case v => throw new RuntimeException(s"Unexpected content value: $v")
    }

  override def startUp(): Future[Unit] = {
    val fs = MysqlJobStore.Ddl.map(ddl => client.query(ddl).unit)
    Future.join(fs)
  }

  override def close(deadline: Time): Future[Unit] = client.close(deadline)

  private def sqlSet(elements: Iterable[_]) = '(' + Seq.fill(elements.size)("?").mkString(", ") + ')'
}

object MysqlJobStore {
  private val Ddl = Seq(
    "create table if not exists jobs(" +
      "unused_id int not null auto_increment," +
      "name varchar(255) not null," +
      "state varchar(15) not null," +
      "owner varchar(255) null," +
      "create_time bigint not null," +
      // Mediumblob is up to 16Mb.
      "content mediumblob not null," +
      "primary key (unused_id)," +
      "unique key uix_name(name)" +
      ") engine=InnoDB default charset=utf8",
    "create table if not exists jobs_labels(" +
      "unused_id int not null auto_increment," +
      "name varchar(255) not null," +
      "label_key varchar(255) not null," +
      "label_value varchar(255) not null," +
      "primary key (unused_id)," +
      "key uix_name_label_key(name, label_key)" +
      ") engine=InnoDB default charset=utf8")

  private val GetQuery = "select content from jobs where name = ?"
  private val DeleteQuery = "delete from jobs where name = ?"
  private val ReplaceQuery = "update jobs set state = ?, content = ? where name = ?"
  private val InsertQuery = "insert into jobs(name, create_time, state, owner, content) values(?, ?, ?, ?, ?)"
  private val InsertLabelQuery = "insert into jobs_labels(name, label_key, label_value) values(?, ?, ?)"
}