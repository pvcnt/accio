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

package fr.cnrs.liris.accio.core.storage

import java.util.concurrent.locks.ReentrantReadWriteLock

import com.google.common.util.concurrent.{AbstractIdleService, ServiceManager}

import scala.collection.JavaConverters._

private[storage] trait AbstractStorage extends AbstractIdleService with Storage {
  private[this] val serviceManager = new ServiceManager(Set(runRepository, workflowRepository, logRepository).asJava)
  private[this] val lock = new ReentrantReadWriteLock()

  override final def read[T](fn: RepositoryProvider => T): T = {
    enforceRunning()
    lock.readLock.lock()
    //try {
    fn(new RepositoryProvider {
      override def logs: LogRepository = logRepository

      override def runs: RunRepository = runRepository

      override def workflows: WorkflowRepository = workflowRepository
    })
    //} finally {
    //lock.readLock.unlock()
    //}
  }

  override final def write[T](fn: MutableRepositoryProvider => T): T = {
    enforceRunning()
    lock.writeLock.lock()
    try {
      fn(new MutableRepositoryProvider {
        override def logs: MutableLogRepository = logRepository

        override def runs: MutableRunRepository = runRepository

        override def workflows: MutableWorkflowRepository = workflowRepository
      })
    } finally {
      lock.writeLock.unlock()
    }
  }

  protected def runRepository: MutableRunRepository

  protected def workflowRepository: MutableWorkflowRepository

  protected def logRepository: MutableLogRepository

  override protected def shutDown(): Unit = {
    serviceManager.stopAsync().awaitStopped()
  }

  override protected def startUp(): Unit = {
    serviceManager.startAsync().awaitHealthy()
  }

  private def enforceRunning() = synchronized {
    if (!isRunning) {
      startAsync().awaitRunning()
    }
  }
}