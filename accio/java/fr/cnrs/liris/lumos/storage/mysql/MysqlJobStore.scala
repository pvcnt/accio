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
import com.twitter.util.{Future, StorageUnit}
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.domain.{Job, JobList, thrift}
import fr.cnrs.liris.lumos.storage.{JobQuery, JobStore, WriteResult}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.mutable

private[storage] final class MysqlJobStore(client: Client, statsReceiver: StatsReceiver)
  extends JobStore {

  private[this] val sizeStat = statsReceiver.stat("storage", "job", "content_size_kb")

  override def create(job: Job): Future[WriteResult] = {
    val content = encode(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare("insert into jobs(name, state, owner, content) values(?, ?, ?, ?)")
      .apply(
        job.name,
        job.status.state.name,
        job.owner.map(wrap(_)).getOrElse(NullParameter),
        content)
      .flatMap {
        case _: OK =>
          val fs = job.labels.toSeq.map { case (k, v) =>
            client
              .prepare("insert into jobs_labels(name, label_key, label_value) values(?, ?, ?)")
              .apply(job.name, k, v)
          }
          Future
            .join(fs)
            .map(_ => WriteResult.Ok)
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
      .handle {
        // Error code 1062 corresponds to a duplicate entry, which means the object already exists.
        case ServerError(1062, _, _) => WriteResult.AlreadyExists
      }
  }

  override def replace(job: Job): Future[WriteResult] = {
    // Owner and labels are both immutable, so we do not update their associated denormalizations.
    val content = encode(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare("update jobs set state = ?, content = ? where name = ?")
      .apply(job.status.state.name, content, job.name)
      .map {
        case ok: OK => if (ok.affectedRows == 1) WriteResult.Ok else WriteResult.NotFound
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
  }

  override def delete(name: String): Future[WriteResult] = {
    client
      .prepare("delete from jobs where name = ?")
      .apply(name)
      .map {
        case ok: OK => if (ok.affectedRows == 1) WriteResult.Ok else WriteResult.NotFound
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
    query.state.foreach { state =>
      where += "state = ?"
      params += state.name
    }
    query.labels.foreach { selector =>
      // TODO
    }

    val sql = s"select content from jobs where ${if (where.nonEmpty) where.mkString(" and ") else "true"}"
    val sql2 = s"select count(1) from jobs where ${if (where.nonEmpty) where.mkString(" and ") else "true"}"
    Future.join(
      client.prepare(sql).select(params: _*)(decodeJob),
      client.prepare(sql2).select(params: _*)(decodeCount).map(_.head))
      .map { case (jobs, totalCount) => JobList(jobs, totalCount) }
  }

  override def get(name: String): Future[Option[Job]] = {
    client
      .prepare("select content from jobs where name = ?")
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

  override def shutDown(): Future[Unit] = client.close()
}

object MysqlJobStore {
  private val Ddl = Seq(
    "create table if not exists jobs(" +
      "unused_id int not null auto_increment," +
      "name varchar(255) not null," +
      "state varchar(15) not null," +
      "owner varchar(255) null," +
      // Mediumblob is up to 16Mb. The `max_allowed_packet` parameter has to be set accordingly.
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
      "key uix_name(job_name, label_key)" +
      ") engine=InnoDB default charset=utf8")
}