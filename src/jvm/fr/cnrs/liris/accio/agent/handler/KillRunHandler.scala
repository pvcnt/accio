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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.{KillRunRequest, KillRunResponse}
import fr.cnrs.liris.accio.framework.api.thrift.{Run, UnknownRunException}
import fr.cnrs.liris.accio.framework.scheduler.standalone.ClusterState
import fr.cnrs.liris.accio.framework.scheduler.{EventType, Scheduler}
import fr.cnrs.liris.accio.framework.service.RunManager
import fr.cnrs.liris.accio.framework.storage.Storage
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler

/**
 * Kill a run and all running tasks.
 *
 * @param storage    Storage.
 * @param state      Cluster state.
 * @param scheduler  Scheduler.
 * @param runManager Run manager.
 */
final class KillRunHandler @Inject()(
  storage: Storage,
  state: ClusterState,
  scheduler: Scheduler,
  runManager: RunManager)
  extends AbstractHandler[KillRunRequest, KillRunResponse] {

  @throws[UnknownRunException]
  override def handle(req: KillRunRequest): Future[KillRunResponse] = {
    val newRun = storage.runs.transactional(req.id) {
      case None => throw UnknownRunException(req.id)
      case Some(run) =>
        if (run.children.nonEmpty) {
          var newParent = run
          // If is a parent run, kill child all runs.
          run.children.foreach { childId =>
            storage.runs.foreach(childId) { child =>
              val res = cancelRun(child, Some(run))
              storage.runs.save(res._1)
              res._2.foreach(p => newParent = p)
            }
          }
          storage.runs.save(newParent)
          newParent
        } else {
          run.parent match {
            case None =>
              val (newRun, _) = cancelRun(run, None)
              storage.runs.save(newRun)
              newRun
            case Some(parentId) =>
              var newRun = run
              storage.runs.foreach(parentId) { parent =>
                val res = cancelRun(run, Some(parent))
                newRun = res._1
                storage.runs.save(newRun)
                res._2.foreach(storage.runs.save)
              }
              newRun
          }
        }
    }
    Future(KillRunResponse(newRun))
  }

  private def cancelRun(run: Run, parent: Option[Run]) = {
    val killedTasks = scheduler.kill(run.id)
    scheduler.houseKeeping(EventType.MoreResource)
    runManager.onKill(run, killedTasks.map(_.nodeName), None)
  }
}