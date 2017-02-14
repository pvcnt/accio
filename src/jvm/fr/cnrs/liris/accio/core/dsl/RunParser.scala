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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.validation.Min
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.runtime.{BaseFactory, RunFactory}
import fr.cnrs.liris.accio.core.storage.WorkflowRepository
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}

import scala.collection.mutable
import scala.util.control.NonFatal

class RunParser(mapper: FinatraObjectMapper, workflowRepository: WorkflowRepository, factory: RunFactory)
  extends BaseFactory with LazyLogging {

  @throws[InvalidSpecException]
  def parse(content: String, params: Map[String, String], warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): RunSpec = {
    var (workflow, spec) = if (content.startsWith("{")) {
      // Content begins with a brace, it should be a JSON object.
      parseWorkflow(content, warnings)
    } else {
      // Otherwise, we consider content is a workflow specification.
      val workflow = findWorkflow(new String(content), warnings)
      val spec = RunSpec(Package(workflow.id, workflow.version))
      (workflow, spec)
    }

    // Add additional parameters to base run specifications.
    spec = spec.copy(params = spec.params ++ parseParams(workflow, params, warnings))

    // Validate the specification would generate valid run(s).
    val validationResult = factory.validate(spec)
    validationResult.maybeThrowException()
    warnings ++= validationResult.warnings

    spec
  }

  /**
   * Parse string parameters into values.
   *
   * @param workflow Workflow this parameters apply to.
   * @param params   Mapping between parameter names and string values.
   * @param warnings Mutable list collecting warnings.
   */
  private def parseParams(workflow: Workflow, params: Map[String, String], warnings: mutable.Set[InvalidSpecMessage]) = {
    params.map { case (paramName, strValue) =>
      workflow.params.find(_.name == paramName) match {
        case None => throw newError(s"Unknown param: $paramName", warnings)
        case Some(param) =>
          val value = try {
            Values.encode(Values.parse(strValue, param.kind), param.kind)
          } catch {
            case NonFatal(_) => throw newError(s"Cannot parse param $paramName as ${DataTypes.toString(param.kind)}", warnings)
          }
          paramName -> Seq(value)
      }
    }
  }

  private def findWorkflow(str: String, warnings: mutable.Set[InvalidSpecMessage]): Workflow = {
    val maybeWorkflow = str.split(":") match {
      case Array(id) => workflowRepository.get(WorkflowId(id))
      case Array(id, version) => workflowRepository.get(WorkflowId(id), version)
      case _ => throw newError(s"Invalid workflow specification: $str", warnings)
    }
    maybeWorkflow match {
      case None => throw newError(s"Workflow not found: $str", warnings)
      case Some(workflow) => workflow
    }
  }

  private def parseWorkflow(content: String, warnings: mutable.Set[InvalidSpecMessage]) = {
    val json = try {
      mapper.parse[JsonRunDef](content)
    } catch {
      case e: JsonParseException => throw newError(s"Parse error: ${e.getMessage}", warnings)
      case e: JsonMappingException => throw newError(s"Mapping error: ${e.getMessage}", e.getPathReference, warnings)
      case NonFatal(e) =>
        // This is not an exception that was supposed to be thrown by the object mapper.
        logger.error("Unexpected parse error", e)
        throw newError(s"Unexpected error: ${e.getMessage}", warnings)
    }

    val workflow = findWorkflow(json.workflow, warnings)
    val baseParams = json.params.map { case (paramName, explo) =>
      workflow.params.find(_.name == paramName) match {
        case None => throw newError(s"Unknown param", s"params.$paramName", warnings)
        case Some(param) =>
          val values = try {
            explo.expand(param.kind).toSeq
          } catch {
            case e: IllegalArgumentException =>
              throw newError(s"Cannot parse value: ${e.getMessage}", s"params.$paramName", warnings)
          }
          paramName -> values
      }
    }
    val spec = RunSpec(
      pkg = Package(workflow.id, workflow.version),
      owner = None,
      name = json.name,
      notes = json.notes,
      tags = json.tags.toSet,
      seed = json.seed,
      params = baseParams,
      repeat = json.repeat)
    (workflow, spec)
  }
}


private case class JsonRunDef(
  workflow: String,
  name: Option[String],
  notes: Option[String],
  tags: Seq[String] = Seq.empty,
  seed: Option[Long],
  params: Map[String, Exploration] = Map.empty,
  @Min(1) repeat: Option[Int])