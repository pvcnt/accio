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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.config.AgentConfig
import fr.cnrs.liris.accio.agent.{ListAgentsRequest, ListAgentsResponse}
import fr.cnrs.liris.accio.core.domain.{Agent, Resource, WorkerId}
import fr.cnrs.liris.accio.core.scheduler.ClusterState

class ListAgentsHandler @Inject()(state: ClusterState, config: AgentConfig)
  extends AbstractHandler[ListAgentsRequest, ListAgentsResponse] {

  override def handle(req: ListAgentsRequest): Future[ListAgentsResponse] = {
    val workers = state.read(identity).map(worker => Agent(worker.id, Some(worker.dest), isMaster = false, isWorker = true, worker.registeredAt.inMillis, worker.maxResources))
    val master = Agent(WorkerId(config.name), None, isMaster = true, isWorker = false, 0, Resource(0, 0, 0))

    Future(ListAgentsResponse(Seq(master) ++ workers))
  }
}