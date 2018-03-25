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

package fr.cnrs.liris.accio.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.util.{Future, FuturePool}

import scala.collection.JavaConverters._

final class ThreadManager(pool: FuturePool) {
  private[this] val counter = new AtomicInteger
  //TODO: should clean this when a thread was killed by a direct call to its kill() method.
  //Now, we are keeping a reference to every ever created thread.
  private[this] val threads = new ConcurrentHashMap[String, ThreadLike[_]].asScala

  def submit[T](thread: ThreadLike[T]): Future[T] = submit(thread, counter.incrementAndGet().toString)

  def submit[T](thread: ThreadLike[T], key: String): Future[T] = {
    threads(key) = thread
    pool(thread.run())
  }

  def kill(key: String): Boolean = {
    threads.remove(key) match {
      case Some(thread) =>
        thread.kill()
        true
      case None => false
    }
  }

  def killAll(): Unit = threads.keySet.foreach(kill)
}