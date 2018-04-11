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

import com.twitter.finagle.mysql._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.util.{Await, StorageUnit}
import fr.cnrs.liris.accio.api.ResultList
import fr.cnrs.liris.accio.api.thrift.{NodeStatus, Run}
import fr.cnrs.liris.accio.storage.{RunQuery, RunStore}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

private[mysql] final class MysqlRunStore(client: Client, statsReceiver: StatsReceiver)
  extends RunStore.Mutable {

  private[this] val sizeStat = statsReceiver.stat("storage", "mysql", "run", "content_size_kb")

  override def save(run: Run): Unit = {
    val content = BinaryScroogeSerializer.toBytes(run)
    sizeStat.add(StorageUnit.fromBytes(content.length).inKilobytes)
    val f = client
      .prepare("insert into runs(id, content) values(?, ?) on duplicate key update content = ?")
      .apply(run.id, content, content)
    Await.result(f)
  }

  override def delete(id: String): Unit = {
    val f = client
      .prepare("delete from runs where id = ?")
      .apply(id)
    Await.result(f)
  }

  override def list(query: RunQuery): ResultList[Run] = {
    val f = client
      .prepare(s"select content from runs")
      .select()(decode)
      .map { runs =>
        val results = runs
          .filter(query.matches)
          .sortWith((a, b) => a.createdAt > b.createdAt)
        ResultList.slice(results, offset = query.offset, limit = query.limit)
      }
    Await.result(f)
  }

  override def get(id: String): Option[Run] = {
    val f = client
      .prepare("select content from runs where id = ?")
      .select(id)(decode)
      .map(_.headOption)
    Await.result(f)
  }

  override def fetch(cacheKey: String): Option[NodeStatus] = None

  private def decode(row: Row): Run =
    row("content").get match {
      case raw: RawValue => BinaryScroogeSerializer.fromBytes(raw.bytes, Run)
      case v => throw new RuntimeException(s"Unexpected content value: $v")
    }
}
