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

package fr.cnrs.liris.accio.client.parser

import java.io.FileInputStream
import java.nio.file.Path

import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.validation.Min
import com.twitter.util.Await
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service.handler.GetWorkflowRequest

class RunTemplateParser @Inject()(mapper: FinatraObjectMapper) {
  def parse(path: Path): ClientRunTemplate = {
    val fis = new FileInputStream(path.toFile)
    try {
      mapper.parse[ClientRunTemplate](fis)
    } finally {
      fis.close()
    }
  }
}

class RunTemplateFactory @Inject()(agentClient: AgentService.FinagledClient) {
  def create(partials: ClientRunTemplate*): RunTemplate = {
    val workflow = getWorkflow(partials.flatMap(_.pkg).lastOption)
    val params = partials.flatMap(_.params.toSeq).map { case (paramName, explo) =>
      workflow.params.find(_.name == paramName) match {
        case None => throw new IllegalArgumentException(s"Unknown param: $paramName")
        case Some(param) => paramName -> explo.expand(param.kind).toSeq
      }
    }.toMap
    val environment = partials.flatMap(_.environment).lastOption
    val name = partials.flatMap(_.name).lastOption
    val notes = partials.flatMap(_.notes).lastOption
    val tags = partials.flatMap(_.tags).toSet
    val seed = partials.flatMap(_.seed).lastOption
    val repeat = partials.flatMap(_.repeat).lastOption
    RunTemplate(
      pkg = Package(workflow.id, workflow.version),
      cluster = "default",
      environment = environment,
      owner = None,
      name = name,
      notes = notes,
      tags = tags,
      seed = seed,
      params = params,
      repeat = repeat)
  }

  private def getWorkflow(maybeStr: Option[String]): Workflow = {
    maybeStr match {
      case None => throw new IllegalArgumentException("You must specify a package")
      case Some(str) =>
        val f = str.split(":") match {
          case Array(id) => agentClient.getWorkflow(GetWorkflowRequest(WorkflowId(id)))
          case Array(id, version) => agentClient.getWorkflow(GetWorkflowRequest(WorkflowId(id), Some(version)))
          case _ => throw new IllegalArgumentException(s"Invalid workflow specification: $str")
        }
        val maybeWorkflow = Await.result(f).result
        maybeWorkflow match {
          case None => throw new IllegalArgumentException(s"Unknown workflow: $str")
          case Some(workflow) => workflow
        }
    }
  }
}

case class ClientRunTemplate(
  pkg: Option[String],
  environment: Option[String],
  name: Option[String],
  notes: Option[String],
  tags: Set[String] = Set.empty,
  seed: Option[Long],
  params: Map[String, Exploration] = Map.empty,
  @Min(1) repeat: Option[Int])

