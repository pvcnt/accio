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

package fr.cnrs.liris.lumos.state

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{Lock, ReentrantLock}

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Future
import fr.cnrs.liris.lumos.domain.{Event, Job, Status}
import fr.cnrs.liris.lumos.storage.JobStore

import scala.collection.JavaConverters._

@Singleton
final class EventHandler @Inject()(jobStore: JobStore) {
  private[this] val locks = new ConcurrentHashMap[String, Lock].asScala

  def handle(event: Event): Future[Status] = {
    locks.putIfAbsent(event.parent, new ReentrantLock)
    val lock = locks(event.parent)
    lock.lock()
    try {
      jobStore.get(event.parent).flatMap {
        case None =>
          JobStateMachine.apply(Job(), event) match {
            case Right(newJob) => jobStore.create(newJob)
            case Left(status) => Future.value(status)
          }
        case Some(job) =>
          JobStateMachine.apply(job, event) match {
            case Right(newJob) => jobStore.replace(newJob)
            case Left(status) => Future.value(status)
          }
      }
    } finally {
      lock.unlock()
    }
  }
}
