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
import fr.cnrs.liris.accio.core.domain.{MutableRunRepository, Task, TaskStatus}
import fr.cnrs.liris.accio.core.service.{RunLifecycleManager, StateManager}

/**
 * Observer tracking lost tasks.
 */
class LostTaskObserver(taskTimeout: Duration, stateManager: StateManager, runRepository: MutableRunRepository, runManager: RunLifecycleManager)
  extends Runnable with StrictLogging {

  private[this] val killed = new AtomicBoolean(false)

  override def run(): Unit = {
    logger.debug("Started lost tasks thread")
    while (!killed.get) {
      val lock = stateManager.lock("lost-tasks")
      if (lock.tryLock()) {
        try {
          lostTasks.foreach(handleLostTask)
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
    val runLock = stateManager.lock(s"run/${task.runId.value}")
    runLock.lock()
    try {
      runRepository.get(task.runId).foreach { run =>
        val newRun = runManager.onLost(run, task.nodeName)
        runRepository.save(newRun)
      }
    } finally {
      runLock.unlock()
    }
    stateManager.remove(task.id)
    logger.debug(s"[T${task.id.value}] Lost task")
  }
}