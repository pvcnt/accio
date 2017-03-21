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
import fr.cnrs.liris.accio.agent.{DeleteRunRequest, DeleteRunResponse}
import fr.cnrs.liris.accio.core.domain.{RunId, UnknownRunException}
import fr.cnrs.liris.accio.core.scheduler.{ClusterState, EventType, Scheduler}
import fr.cnrs.liris.accio.core.storage.Storage

/**
 * Delete a run, and all its children if it is a parent run.
 *
 * @param storage   Storage.
 * @param scheduler Scheduler.
 * @param state     Cluster state.
 */
final class DeleteRunHandler @Inject()(
  storage: Storage,
  scheduler: Scheduler,
  state: ClusterState)
  extends AbstractHandler[DeleteRunRequest, DeleteRunResponse] {

  @throws[UnknownRunException]
  override def handle(req: DeleteRunRequest): Future[DeleteRunResponse] = {
    storage.write { provider =>
      provider.runs.get(req.id) match {
        case None => throw UnknownRunException(req.id)
        case Some(run) =>
          if (run.children.nonEmpty) {
            // It is a parent run, cancel and delete child all runs.
            cancelRuns(run.children)
            run.children.foreach { runId =>
              provider.runs.remove(runId)
              provider.logs.remove(runId)
            }
          } else if (run.parent.isDefined) {
            // It is a child run, update or delete parent run.
            provider.runs.get(run.parent.get).foreach { parent =>
              if (parent.children.size > 1) {
                // There are several child runs left, remove current one from the list.
                provider.runs.save(parent.copy(children = parent.children.filterNot(_ == run.id)))
              } else {
                // It was the last child of this run, delete it as it is now useless.
                provider.runs.remove(parent.id)
              }
            }
          } else {
            // It is a single run, cancel it.
            cancelRuns(Seq(run.id))
          }
          // Finally, delete the run.
          provider.runs.remove(run.id)
          provider.logs.remove(run.id)
      }
    }
    Future(DeleteRunResponse())
  }

  private def cancelRuns(ids: Seq[RunId]) = {
    ids.foreach(scheduler.kill(_))
    scheduler.houseKeeping(EventType.MoreResource)
  }
}