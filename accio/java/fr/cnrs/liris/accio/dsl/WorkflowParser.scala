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

package fr.cnrs.liris.accio.dsl

import java.io.File

import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future
import fr.cnrs.liris.accio.api._
import fr.cnrs.liris.util.HashUtils

/**
 * Parse workflow specification DSL.
 */
final class WorkflowParser {
  def parse(file: File): Future[thrift.Workflow] = {
    Reader
      .readAll(Reader.fromFile(file))
      .map { case Buf.Utf8(content) => parse(content, Some(file.getName)) }
  }

  /**
   * Parse a string into a workflow.
   *
   * @param content  Workflow DSL, as string.
   * @param filename Name of the file from which the DSL is extracted.
   */
  def parse(content: String, filename: Option[String]): thrift.Workflow = {
    val json = mapper.parse[WorkflowDsl](content)
    val id = json.id.orElse(filename.map(defaultId)).getOrElse(HashUtils.sha1(content))
    val owner = json.owner.map(Utils.parseUser)
    val nodes = json.graph.map(getNode)
    val params = json.params.map { paramDef =>
      val kind = DataTypes.parse(paramDef.kind)
      val defaultValue = paramDef.defaultValue.flatMap(Values.as(_, kind))
      thrift.ArgDef(name = paramDef.name, kind = kind, defaultValue = defaultValue, help = paramDef.help)
    }
    thrift.Workflow(
      id = id,
      name = json.name,
      owner = owner,
      nodes = nodes,
      params = params)
  }

  private def defaultId(filename: String) = filename.substring(0, filename.lastIndexOf("."))

  private def getNode(node: NodeDsl): thrift.Node = {
    val inputs = node.inputs.map { case (name, value) =>
      val input = value match {
        case InputDsl.Value(rawValue) => thrift.Input.Value(rawValue)
        case InputDsl.Reference(ref) => thrift.Input.Reference(References.parse(ref))
        case InputDsl.Param(paramName) => thrift.Input.Param(paramName)
      }
      name -> input
    }
    thrift.Node(node.op, node.name, inputs)
  }

  private[this] def mapper = ObjectMapperFactory.default
}