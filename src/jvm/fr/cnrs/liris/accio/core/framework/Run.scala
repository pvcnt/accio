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

import com.google.common.base.MoreObjects

/**
 * A run is a particular instantiation of a graph, where everything is well defined (i.e., all parameters are fixed and
 * have a single value). A run always belongs to an experiment. We do *not* include the graph definition here because
 * it is already embedded in the experiment definition.
 *
 * @param id     Unique identifier (among all runs AND experiments).
 * @param parent Parent experiment identifier.
 * @param name   Human-readable name.
 * @param seed   Seed used by unstable operators.
 * @param params Values of workflow parameters in this run.
 * @param report Execution report.
 */
case class Run(
  id: String,
  parent: String,
  name: String,
  seed: Long,
  params: Map[String, Any],
  report: Option[RunReport] = None) {

  def shortId: String = id.substring(0, 8)

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("name", name)
      .add("params", params.map { case (k, v) => s"$k=$v" }.mkString(", "))
      .toString
}