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
import fr.cnrs.liris.accio.agent.{KillRunRequest, KillRunResponse, UnknownRunException}
import fr.cnrs.liris.accio.core.domain.Run
import fr.cnrs.liris.accio.core.runtime.RunManager
import fr.cnrs.liris.accio.core.scheduler.Scheduler
import fr.cnrs.liris.accio.core.statemgr.{LockService, StateManager}
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * Kill a run and all running tasks.
 *
 * @param runRepository Run repository.
 * @param stateManager  State manager.
 * @param scheduler     Scheduler.
 * @param runManager    Run manager.
 * @param lockService   Lock service.
 */
final class KillRunHandler @Inject()(
  runRepository: MutableRunRepository,
  stateManager: StateManager,
  scheduler: Scheduler,
  runManager: RunManager,
  lockService: LockService) extends Handler[KillRunRequest, KillRunResponse] {

  override def handle(req: KillRunRequest): Future[KillRunResponse] = {
    lockService.withLock(req.id) {
      runRepository.get(req.id) match {
        case None => throw new UnknownRunException
        case Some(run) =>
          if (run.children.nonEmpty) {
            var newParent = run
            // If is a parent run, kill child all runs.
            run.children.foreach { childId =>
              lockService.withLock(childId) {
                runRepository.get(childId).foreach { child =>
                  val res = cancelRun(child, Some(run))
                  runRepository.save(res._1)
                  res._2.foreach(p => newParent = p)
                }
              }
            }
            runRepository.save(newParent)
          } else {
            lockService.withLock(run.parent) {
              val (newRun, newParent) = cancelRun(run, run.parent.flatMap(runRepository.get))
              runRepository.save(newRun)
              newParent.foreach(runRepository.save)
            }
          }
      }
    }
    Future(KillRunResponse())
  }

  private def cancelRun(run: Run, parent: Option[Run]) = {
    val tasks = stateManager.tasks.filter(_.runId == run.id)
    tasks.foreach { task =>
      lockService.withLock(task.id) {
        scheduler.kill(task.key)
        stateManager.remove(task.id)
      }
    }
    runManager.onKill(run, tasks.map(_.nodeName), None)
  }
}
