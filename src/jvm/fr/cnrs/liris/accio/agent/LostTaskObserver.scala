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

package fr.cnrs.liris.accio.agent

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.util.{Duration, Time}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.domain.{Run, Task, TaskStatus}
import fr.cnrs.liris.accio.core.runtime.RunManager
import fr.cnrs.liris.accio.core.statemgr.StateManager
import fr.cnrs.liris.accio.core.storage.MutableRunRepository

/**
 * Observer tracking lost tasks.
 */
final class LostTaskObserver(
  taskTimeout: Duration,
  stateManager: StateManager,
  runRepository: MutableRunRepository,
  runManager: RunManager)
  extends Runnable with StrictLogging {
  // Flag to indicate when this has been killed.
  private[this] val killed = new AtomicBoolean(false)

  override def run(): Unit = {
    logger.debug("Started lost tasks thread")
    while (!killed.get) {
      val lock = stateManager.lock("lost-tasks")
      if (lock.tryLock()) {
        try {
          val lock = stateManager.lock("write")
          lock.lock()
          try {
            lostTasks.foreach(handleLostTask)
          } finally {
            lock.unlock()
          }
        } finally {
          lock.unlock()
        }
      }
      try {
        Thread.sleep(20 * 1000)
      } catch {
        case _: InterruptedException => // Do nothing.
      }
    }
    logger.debug("Stopped lost tasks thread")
  }

  def kill(): Unit = {
    killed.set(true)
  }

  private def lostTasks = {
    val deadline = Time.now - taskTimeout
    stateManager.tasks.filterNot(isActive(_, deadline))
  }

  private def isActive(task: Task, deadline: Time) = {
    task.state.status == TaskStatus.Scheduled || task.state.heartbeatAt.exists(_ >= deadline.inMillis)
  }

  private def handleLostTask(task: Task) = {
    runRepository.get(task.runId).foreach { run =>
      run.parent match {
        case Some(parentId) => processRun(run, task, runRepository.get(parentId))
        case None => processRun(run, task, None)
      }
    }
    stateManager.remove(task.id)
    logger.debug(s"[T${task.id.value}] Lost task")
  }

  private def processRun(run: Run, task: Task, parent: Option[Run]) = {
    val (newRun, newParent) = runManager.onLost(run, task.nodeName, parent)
    runRepository.save(newRun)
    newParent.foreach(runRepository.save)
  }
}