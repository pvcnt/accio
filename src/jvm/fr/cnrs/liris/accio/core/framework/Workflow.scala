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

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A workflow is a named graph of operators.
 *
 * @param graph  Graph of operators.
 * @param owner  User owning this workflow.
 * @param name   Human-readable name.
 * @param params Workflow parameters.
 */
case class Workflow(graph: Graph, owner: User, name: String, params: Set[Param])

/**
 * A parameter is a workflow-level input. A parameter can be used in multiple ports, as long as they are of the same
 * data type.
 *
 * @param name       Parameter name.
 * @param kind       Data type.
 * @param isOptional Whether it is optional and does not have to be specified.
 * @param ports      References to ports using this parameter.
 */
case class Param(name: String, @JsonProperty("type") kind: DataType, isOptional: Boolean, ports: Set[Reference])

/**
 * Utils for [[Param]].
 */
object Param {
  /**
   * Pattern for valid param names.
   */
  val NamePattern = "[a-zA-Z0-9_]+"

  /**
   * Regex for valid param names.
   */
  val NameRegex = ("^" + NamePattern + "$").r
}

/**
 * Definition of workflow.
 *
 * @param graph Definition of the graph of operators.
 * @param owner User owning this workflow.
 * @param name  Human-readable name.
 */
case class WorkflowDef(graph: GraphDef, owner: Option[User] = None, name: Option[String] = None)