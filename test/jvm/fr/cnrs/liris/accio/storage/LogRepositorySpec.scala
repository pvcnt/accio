/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.storage

import java.util.UUID

import com.twitter.util.Time
import fr.cnrs.liris.accio.api.thrift._

/**
 * Common unit tests for all [[MutableLogRepository]] implementations, ensuring they all have consistent behavior.
 */
private[storage] abstract class LogRepositorySpec extends RepositorySpec[MutableLogRepository] {
  protected def refreshBeforeSearch(): Unit = {}

  it should "save logs" in {
    val runIds = Seq.fill(5)(randomId)
    val now = System.currentTimeMillis()
    val logs = runIds.map { runId =>
      runId -> Seq.tabulate(3) { i =>
        s"Node$i" -> (Seq.tabulate(10) { j =>
          RunLog(runId, s"Node$i", now + i * 25 + j, "stdout", s"line $i $j")
        } ++ Seq.tabulate(15) { j =>
          RunLog(runId, s"Node$i", now + i * 25 + 10 + j, "stderr", s"line $i $j")
        })
      }.toMap
    }.toMap
    repo.save(logs.values.flatMap(_.values).flatten.toSeq)
    refreshBeforeSearch()

    var res = repo.find(LogsQuery(runIds.head, "Node2"))
    res should contain theSameElementsAs logs(runIds.head)("Node2")

    res = repo.find(LogsQuery(runIds.head, "Node2", classifier = Some("stdout")))
    res should contain theSameElementsAs logs(runIds.head)("Node2").take(10)

    res = repo.find(LogsQuery(runIds.head, "Node2", limit = Some(10)))
    res should have size 10

    res = repo.find(LogsQuery(runIds.last, "Node0", since = Some(Time.fromMilliseconds(now + 15))))
    res should contain theSameElementsAs logs(runIds.last)("Node0").drop(16)
  }

  private def randomId: RunId = RunId(UUID.randomUUID().toString)
}