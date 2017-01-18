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

package fr.cnrs.liris.accio.client.service

import java.nio.file.{Path, Paths}

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Throw}
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.handler.GetWorkflowRequest

import scala.util.control.NonFatal

class RunDefFactory @Inject()(parser: JsonRunDefParser, client: AgentService.FinagledClient) {
  @throws[ParsingException]
  @throws[InvalidRunDefException]
  @throws[AccioServerException]
  def create(uri: String, name: Option[String] = None, notes: Option[String] = None, tags: Set[String] = Set.empty,
    repeat: Option[Int] = None, seed: Option[Long] = None, params: Map[String, String] = Map.empty): RunDef = {

    val path = Paths.get(uri)
    var (workflow, defn) = if (path.toFile.exists) {
      parseWorkflow(path)
    } else {
      val workflow = findWorkflow(uri)
      (workflow, RunDef(Package(workflow.id, workflow.version)))
    }
    name.foreach { name =>
      defn = defn.copy(name = Some(name))
    }
    notes.foreach { notes =>
      defn = defn.copy(notes = Some(notes))
    }
    if (tags.nonEmpty) {
      defn = defn.copy(tags = defn.tags ++ tags)
    }
    repeat.foreach { repeat =>
      defn = defn.copy(repeat = Some(repeat))
    }
    seed.foreach { seed =>
      defn = defn.copy(seed = Some(seed))
    }
    if (params.nonEmpty) {
      val newParams = defn.params ++ parseParams(params, workflow)
      defn = defn.copy(params = newParams)
    }
    defn
  }

  private def parseWorkflow(path: Path) = {
    val json = parser.parse(path)
    val workflow = findWorkflow(json.pkg)
    val params = json.params.map { case (paramName, explo) =>
      workflow.params.find(_.name == paramName) match {
        case None => throw new InvalidRunDefException(s"Unknown param: $paramName")
        case Some(param) =>
          val values = try {
            explo.expand(param.kind).toSeq
          } catch {
            case e: IllegalArgumentException =>
              throw new InvalidRunDefException(s"Cannot parse exploration of $paramName as ${Utils.describe(param.kind)}", e)
          }
          paramName -> values
      }
    }
    val defn = RunDef(
      pkg = Package(workflow.id, workflow.version),
      owner = None,
      name = json.name,
      notes = json.notes,
      tags = json.tags,
      seed = json.seed,
      params = params,
      repeat = json.repeat)
    (workflow, defn)
  }

  private def parseParams(params: Map[String, String], workflow: Workflow): Map[String, Seq[Value]] =
    params.map { case (paramName, strValue) =>
      workflow.params.find(_.name == paramName) match {
        case None => throw new InvalidRunDefException(s"Unknown param: $paramName")
        case Some(param) =>
          val value = try {
            Values.encode(Values.parse(strValue, param.kind), param.kind)
          } catch {
            case NonFatal(e) => throw new InvalidRunDefException(s"Cannot parse param $paramName as ${Utils.describe(param.kind)}", e)
          }
          paramName -> Seq(value)
      }
    }

  private def findWorkflow(str: String): Workflow = {
    val f = str.split(":") match {
      case Array(id) => client.getWorkflow(GetWorkflowRequest(WorkflowId(id)))
      case Array(id, version) => client.getWorkflow(GetWorkflowRequest(WorkflowId(id), Some(version)))
      case _ => throw new InvalidRunDefException(s"Invalid workflow specification: $str")
    }
    Await.result(f.map(_.result).liftToTry) match {
      case Throw(e) => throw new AccioServerException("Communication error", e)
      case Return(None) => throw new InvalidRunDefException(s"Unknown workflow: $str")
      case Return(Some(workflow)) => workflow
    }
  }
}