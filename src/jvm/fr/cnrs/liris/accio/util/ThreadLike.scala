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

package fr.cnrs.liris.accio.util

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.util.Duration
import com.typesafe.scalalogging.StrictLogging

trait ThreadLike[T] {
  def run(): T

  def kill(): Unit

  protected final def sleep(duration: Duration): Unit = {
    try {
      Thread.sleep(duration.inMillis)
    } catch {
      case _: InterruptedException => // Do nothing.
    }
  }
}

trait InfiniteLoopThreadLike extends ThreadLike[Unit] with StrictLogging {
  private[this] val running = new AtomicBoolean(false)

  override final def run(): Unit = {
    logger.debug(s"${getClass.getSimpleName} started")
    running.set(true)
    sleep(Duration.fromSeconds(5))
    while (running.get) {
      singleOperation()
    }
    logger.debug(s"${getClass.getSimpleName} stopped")
  }

  final def kill(): Unit = {
    running.set(false)
  }

  protected def singleOperation(): Unit
}