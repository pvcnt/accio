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

import fr.cnrs.liris.accio.core.framework.{Experiment, Node, Run}

/**
 * Receives updates as the execution of an experiment progresses.
 */
trait ProgressReporter {
  def onStart(experiment: Experiment): Unit

  def onComplete(experiment: Experiment): Unit

  def onGraphStart(run: Run): Unit

  def onGraphComplete(run: Run): Unit

  def onNodeStart(run: Run, node: Node): Unit

  def onNodeComplete(run: Run, node: Node): Unit
}

/**
 * A progress reporter doing nothing.
 */
object NoProgressReporter extends ProgressReporter {
  override def onStart(experiment: Experiment): Unit = {}

  override def onComplete(experiment: Experiment): Unit = {}

  override def onNodeComplete(run: Run, nodeDef: Node): Unit = {}

  override def onGraphComplete(run: Run): Unit = {}

  override def onGraphStart(run: Run): Unit = {}

  override def onNodeStart(run: Run, nodeDef: Node): Unit = {}
}
