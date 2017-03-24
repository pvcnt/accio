/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.client.controller

import com.twitter.util.{Future, StorageUnit}
import fr.cnrs.liris.accio.agent._

import scala.collection.mutable

class GetAgentController extends AbstractGetController[ListAgentsResponse] with FormatHelper {
  override def retrieve(opts: GetQuery, client: AgentService$FinagleClient): Future[ListAgentsResponse] = {
    client.listAgents(ListAgentsRequest())
  }

  override protected def columns: Seq[(String, Int)] = Seq(
    ("name", 20),
    ("cpu", 4),
    ("ram", 8),
    ("disk", 8),
    ("type", 15))

  override protected def rows(resp: ListAgentsResponse): Seq[Seq[Any]] = {
    resp.results.map { agent =>
      val types = mutable.Set.empty[String]
      if (agent.isMaster) {
        types += "Master"
      }
      if (agent.isWorker) {
        types += "Worker"
      }
      Seq(
        agent.id.value,
        if (agent.isWorker) agent.maxResources.cpu else "-",
        if (agent.isWorker) format(StorageUnit.fromMegabytes(agent.maxResources.ramMb)) else "-",
        if (agent.isWorker) format(StorageUnit.fromMegabytes(agent.maxResources.diskMb)) else "-",
        types.mkString(","))
    }
  }
}