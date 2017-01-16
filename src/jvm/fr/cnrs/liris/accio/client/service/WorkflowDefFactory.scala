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

package fr.cnrs.liris.accio.client.service

import java.nio.file.{Path, Paths}

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.domain._

class WorkflowDefFactory @Inject()(parser: JsonWorkflowDefParser, opRegistry: OpRegistry) extends LazyLogging {
  @throws[ParsingException]
  @throws[InvalidWorkflowDefException]
  def create(uri: String): WorkflowDef = {
    val path = Paths.get(uri)
    val json = parser.parse(path)
    val id = WorkflowId(json.id.getOrElse(defaultId(path)))
    val owner = json.owner.map(Utils.parseUser)
    val nodes = json.graph.map(getNode).toSet

    val params = json.params.map { paramDef =>
      val kind = Utils.parseDataType(paramDef.kind)
      val defaultValue = paramDef.defaultValue.map(Values.encode(_, kind))
      ArgDef(name = paramDef.name, kind = kind, defaultValue = defaultValue)
    }.toSet

    WorkflowDef(
      id = id,
      params = params,
      name = json.name,
      owner = owner,
      graph = GraphDef(nodes))
  }

  private def defaultId(path: Path) = {
    val filename = path.getFileName.toString
    filename.substring(0, filename.indexOf("."))
  }

  private def getNode(node: JsonNodeDef) = {
    val inputs = node.inputs.map {
      case (argName, JsonValueInputDef(rawValue)) =>
        opRegistry.get(node.op) match {
          case Some(opDef) =>
            val value = Values.encode(rawValue, opDef.inputs.find(_.name == argName).get.kind)
            argName -> InputDef.Value(value)
          case None => throw new InvalidWorkflowDefException(s"Unknown operator: ${node.op}")
        }
      case (argName, JsonReferenceInputDef(ref)) => argName -> InputDef.Reference(Utils.parseReference(ref))
      case (argName, JsonParamInputDef(paramName)) => argName -> InputDef.Param(paramName)
    }
    NodeDef(node.op, node.name.getOrElse(node.op), inputs)
  }
}