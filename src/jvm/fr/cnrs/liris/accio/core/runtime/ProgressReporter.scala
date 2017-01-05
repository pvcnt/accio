/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.runtime

import fr.cnrs.liris.accio.core.framework.{Node, NodeStatus, Run, RunStatus}

/**
 * Receives updates as the execution of a run progresses.
 */
trait ProgressReporter {
  def onStart(run: Run): Unit

  def onComplete(run: Run): Unit

  def onStart(run: Run, node: Node): Unit

  def onComplete(run: Run, node: Node): Unit
}

/**
 * A progress reporter doing nothing.
 */
object NoProgressReporter extends ProgressReporter {
  override def onStart(run: Run): Unit = {}

  override def onComplete(run: Run): Unit = {}

  override def onStart(run: Run, nodeDef: Node): Unit = {}

  override def onComplete(run: Run, nodeDef: Node): Unit = {}
}
