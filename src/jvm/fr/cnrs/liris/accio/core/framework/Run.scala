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

package fr.cnrs.liris.accio.core.framework

/**
 * A run is a particular instantiation of a graph, where everything is well defined (i.e., all parameters are fixed and
 * have a single value). A run always belongs to an experiment.
 *
 * @param id     Unique identifier (among all runs AND experiments)
 * @param parent Parent experiment identifier
 * @param graph  Graph being executed
 * @param name   Human-readable name
 * @param idx    Execution order of this run when scheduled to run multiple times.
 * @param seed   Seed used by pseudo-random operators.
 * @param report Execution report
 */
case class Run(
  id: String,
  parent: String,
  graph: Graph,
  name: Option[String],
  idx: Int,
  seed: Long,
  report: Option[RunReport] = None) {

  def shortId: String = id.substring(0, 8)

  override def toString: String = name.getOrElse(shortId)
}