/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.executor

import fr.cnrs.liris.accio.domain.OpResult

trait TaskLifecycleHandler {
  def taskStarted(name: String): Unit

  def taskCompleted(name: String, exitCode: Int, result: OpResult): Unit
}

object NullTaskLifecycleHandler extends TaskLifecycleHandler {
  override def taskStarted(name: String): Unit = {}

  override def taskCompleted(name: String, exitCode: Int, result: OpResult): Unit = {}
}