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
import fr.cnrs.liris.accio.core.runtime.{RunManager, SchedulerService}
import fr.cnrs.liris.accio.core.statemgr.StateManager
import fr.cnrs.liris.accio.core.storage.{MutableRunRepository, MutableTaskRepository, TaskQuery}

/**
 * Kill a run and all running tasks.
 *
 * @param runRepository    Run repository.
 * @param taskRepository   Task repository.
 * @param stateManager     State manager.
 * @param schedulerService Scheduler service.
 * @param runManager       Run manager.
 */
final class KillRunHandler @Inject()(
  runRepository: MutableRunRepository,
  taskRepository: MutableTaskRepository,
  stateManager: StateManager,
  schedulerService: SchedulerService,
  runManager: RunManager)
  extends Handler[KillRunRequest, KillRunResponse] {

  override def handle(req: KillRunRequest): Future[KillRunResponse] = {
    val lock = stateManager.lock("write")
    lock.lock()
    try {
      val newRun = runRepository.get(req.id) match {
        case None => throw new UnknownRunException
        case Some(run) =>
          if (run.children.nonEmpty) {
            var newParent = run
            // If is a parent run, kill child all runs.
            run.children.foreach { childId =>
              runRepository.get(childId).foreach { child =>
                val res = cancelRun(child, Some(run))
                runRepository.save(res._1)
                res._2.foreach(p => newParent = p)
              }
            }
            runRepository.save(newParent)
            newParent
          } else {
            val (newRun, newParent) = cancelRun(run, run.parent.flatMap(runRepository.get))
            runRepository.save(newRun)
            newParent.foreach(runRepository.save)
            newRun
          }
      }
      Future(KillRunResponse(newRun))
    } finally {
      lock.unlock()
    }
  }

  private def cancelRun(run: Run, parent: Option[Run]) = {
    val tasks = taskRepository.find(TaskQuery(runs = Set(run.id)))
    tasks.foreach(schedulerService.kill)
    runManager.onKill(run, tasks.map(_.nodeName).toSet, None)
  }
}
