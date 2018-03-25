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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.api.thrift.InvalidWorkerException
import fr.cnrs.liris.accio.scheduler.standalone.ClusterState
import fr.cnrs.liris.accio.scheduler.{EventType, Scheduler}

/**
 * @param state     Cluster state.
 * @param scheduler Scheduler.
 */
class RegisterWorkerHandler @Inject()(state: ClusterState, scheduler: Scheduler)
  extends AbstractHandler[RegisterWorkerRequest, RegisterWorkerResponse] {

  @throws[InvalidWorkerException]
  override def handle(req: RegisterWorkerRequest): Future[RegisterWorkerResponse] = {
    state.register(req.workerId, req.dest, req.maxResources)
    scheduler.houseKeeping(EventType.MoreResource)
    Future(RegisterWorkerResponse())
  }
}