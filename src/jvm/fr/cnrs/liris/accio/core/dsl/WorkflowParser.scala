/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.dsl

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty, JsonSubTypes}
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.json.FinatraObjectMapper
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.api.{InvalidSpecException, _}
import fr.cnrs.liris.accio.core.framework.{BaseFactory, WorkflowFactory}
import fr.cnrs.liris.accio.core.framework.{BaseFactory, OpRegistry}
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}

import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * Parse workflow specification DSL.
 *
 * @param mapper     Finatra object mapper.
 * @param opRegistry Operator registry.
 */
class WorkflowParser(mapper: FinatraObjectMapper, opRegistry: OpRegistry, factory: WorkflowFactory)
  extends BaseFactory with LazyLogging {
  /**
   *
   * @param content  Workflow DSL, as string.
   * @param filename Name of the file from which the DSL is extracted.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the workflow specification is invalid.
   */
  @throws[InvalidSpecException]
  def parse(content: String, filename: Option[String], warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): WorkflowSpec = {
    val json = parse(content, warnings)
    val id = json.id.orElse(filename.map(defaultId)).getOrElse(throw newError("No workflow identifier", warnings))
    val owner = json.owner.map(Utils.parseUser)
    val nodes = json.graph.map(getNode(_, opRegistry, warnings)).toSet
    val params = json.params.map { paramDef =>
      val kind = DataTypes.parse(paramDef.kind)
      val defaultValue = paramDef.defaultValue.map(Values.encode(_, kind))
      ArgDef(name = paramDef.name, kind = kind, defaultValue = defaultValue)
    }.toSet
    val spec = WorkflowSpec(WorkflowId(id), None, json.name, owner, GraphDef(nodes), params)

    // Validate the specification would generate a valid workflow.
    val validationResult = factory.validate(spec)
    validationResult.maybeThrowException()
    warnings ++= validationResult.warnings

    spec
  }

  private def parse(content: String, warnings: mutable.Set[InvalidSpecMessage]): JsonWorkflowDef =
    try {
      mapper.parse[JsonWorkflowDef](content)
    } catch {
      case e: JsonParseException => throw newError(s"Parse error: ${e.getMessage}", warnings)
      case e: JsonMappingException => throw newError(s"Mapping error: ${e.getMessage}", e.getPathReference, warnings)
      case NonFatal(e) =>
        // This is not an exception that was supposed to be thrown by the object mapper.
        logger.error("Unexpected parse error", e)
        throw newError(s"Unexpected error: ${e.getMessage}", warnings)
    }

  private def defaultId(filename: String) = filename.substring(0, filename.lastIndexOf("."))

  private def getNode(node: JsonNodeDef, opRegistry: OpRegistry, warnings: mutable.Set[InvalidSpecMessage]) = {
    val inputs = node.inputs.flatMap {
      case (argName, JsonValueInputDef(rawValue)) =>
        opRegistry.get(node.op) match {
          case Some(opDef) =>
            opDef.inputs.find(_.name == argName) match {
              case Some(argDef) => Some(argName -> InputDef.Value(Values.encode(rawValue, argDef.kind)))
              case None =>
                warnings += InvalidSpecMessage("Unknown input port", Some(s"graph.${node.name}.inputs.$argName"))
                None
            }
          case None => throw newError(s"Unknown operator: ${node.op}", s"graph.${node.name}.op", warnings)
        }
      case (argName, JsonReferenceInputDef(ref)) => Some(argName -> InputDef.Reference(References.parse(ref)))
      case (argName, JsonParamInputDef(paramName)) => Some(argName -> InputDef.Param(paramName))
    }
    NodeDef(node.op, node.name, inputs)
  }
}

private case class JsonWorkflowDef(
  graph: Seq[JsonNodeDef],
  id: Option[String],
  owner: Option[String],
  name: Option[String],
  params: Seq[JsonArgDef] = Seq.empty)

private case class JsonArgDef(name: String, kind: String, defaultValue: Option[Any])

private case class JsonNodeDef(op: String, @JsonProperty("name") customName: Option[String], inputs: Map[String, JsonInputDef] = Map.empty) {
  def name: String = customName.getOrElse(op)
}

@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[JsonValueInputDef], name = "value"),
  new JsonSubTypes.Type(value = classOf[JsonReferenceInputDef], name = "reference"),
  new JsonSubTypes.Type(value = classOf[JsonParamInputDef], name = "param")))
@JsonIgnoreProperties(ignoreUnknown = true)
private sealed trait JsonInputDef

private case class JsonValueInputDef(value: Any) extends JsonInputDef

private case class JsonReferenceInputDef(reference: String) extends JsonInputDef

private case class JsonParamInputDef(param: String) extends JsonInputDef