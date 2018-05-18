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

package fr.cnrs.liris.accio.storage.mysql

import com.twitter.finagle.mysql.Parameter.{NullParameter, wrap}
import com.twitter.finagle.mysql._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Future, StorageUnit}
import fr.cnrs.liris.accio.api.thrift.Job
import fr.cnrs.liris.accio.storage.{JobStore, JobList}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.collection.mutable

private[mysql] final class MysqlJobStore(client: Client, statsReceiver: StatsReceiver)
  extends JobStore {

  private[this] val sizeStat = statsReceiver.stat("storage", "mysql", "job", "content_size_kb")

  override def create(job: Job): Future[Boolean] = {
    val content = BinaryScroogeSerializer.toBytes(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare("insert into jobs(name, parent, content) values(?, ?, ?)")
      .apply(
        job.name,
        job.parent.map(wrap(_)).getOrElse(NullParameter),
        content)
      .map {
        case _: OK => true
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
      .handle {
        // Error code 1062 corresponds to a duplicate entry, which means the object already exists.
        case ServerError(1062, _, _) => false
      }
  }

  override def replace(job: Job): Future[Boolean] = {
    val content = BinaryScroogeSerializer.toBytes(job)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    client
      .prepare("update jobs set parent = ?, content = ? where name = ?")
      .apply(
        job.parent.map(wrap(_)).getOrElse(NullParameter),
        content,
        job.name)
      .map {
        case ok: OK => ok.affectedRows == 1
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
  }

  override def delete(name: String): Future[Boolean] = {
    client
      .prepare("delete from jobs where name = ?")
      .apply(name)
      .map {
        case ok: OK => ok.affectedRows == 1
        case res => throw new RuntimeException(s"Unexpected MySQL result: $res")
      }
  }

  override def list(query: JobStore.Query, limit: Option[Int], offset: Option[Int]): Future[JobList] = {
    val where = mutable.ListBuffer.empty[String]
    val params = mutable.ListBuffer.empty[Parameter]
    var query2 = query
    query.parent.foreach { parent =>
      if (parent.isEmpty) {
        where += "parent is null"
      } else {
        where += "parent = ?"
        params += parent
        query2 = query2.copy(parent = None)
      }
    }

    val sql = s"select content from jobs where ${if (where.nonEmpty) where.mkString(" and ") else "true"}"
    client
      .prepare(sql)
      .select(params: _*)(decode)
      .map { jobs =>
        val results = jobs
          .filter(query2.matches)
          .sortWith((a, b) => a.createTime > b.createTime)
        JobList.slice(results, offset = offset, limit = limit)
      }
  }

  override def get(name: String): Future[Option[Job]] = {
    client
      .prepare("select content from jobs where name = ?")
      .select(name)(decode)
      .map(_.headOption)
  }

  private def decode(row: Row): Job =
    row("content").get match {
      case raw: RawValue => BinaryScroogeSerializer.fromBytes(raw.bytes, Job)
      case v => throw new RuntimeException(s"Unexpected content value: $v")
    }
}
