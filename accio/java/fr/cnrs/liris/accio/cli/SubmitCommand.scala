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

package fr.cnrs.liris.accio.cli

import com.twitter.util.Future
import fr.cnrs.liris.accio.domain.Workflow
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.dsl.json.JsonWorkflowParser
import fr.cnrs.liris.accio.server.SubmitWorkflowRequest
import fr.cnrs.liris.infra.cli.app.{Environment, ExitCode, Reporter}
import fr.cnrs.liris.infra.thriftserver.{ErrorCode, ServerError}
import fr.cnrs.liris.lumos.domain.{AttrValue, Value}
import fr.cnrs.liris.util.FileUtils

final class SubmitCommand extends AccioCommand {
  private[this] val nameFlag = flag[String]("name", "Job name")
  private[this] val contactFlag = flag[String]("contact", "Job name")
  private[this] val labelsFlag = flag[String]("labels", "Job tags (comma-separated)")
  private[this] val resourcesFlag = flag[String]("resources", "Job tags (comma-separated)")
  private[this] val repeatFlag = flag[Int]("repeat", "Job tags (comma-separated)")
  private[this] val seedFlag = flag[Long]("seed", "Seed to use for controlled randomness")

  override def name = "submit"

  override def help = "Launch an Accio job."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.error("You must provide the path to a job file.")
      return Future.value(ExitCode.CommandLineError)
    }
    parseAndSubmit(residue.head, residue.tail, env)
  }

  private def parseAndSubmit(uri: String, residue: Seq[String], env: Environment): Future[ExitCode] = {
    val file = FileUtils.expandPath(uri).toFile
    if (!file.canRead) {
      env.reporter.error(s"Cannot read workflow definition file: ${file.getAbsolutePath}")
      return Future.value(ExitCode.DefinitionError)
    }
    val client = createAccioClient(env)
    JsonWorkflowParser.default
      .parse(file)
      .map(merge(_, residue))
      .flatMap(job => client.submitWorkflow(SubmitWorkflowRequest(ThriftAdapter.toThrift(job))))
      .map { resp =>
        resp.warnings.foreach { violation =>
          env.reporter.warn(s"${violation.message} (at ${violation.field})")
        }
        env.reporter.info(s"Submitted workflow ${resp.name}")
        ExitCode.Success
      }
  }

  private def merge(workflow: Workflow, residue: Seq[String]) = {
    var res = workflow
    nameFlag.get.foreach(name => res = res.copy(name = name))
    contactFlag.get.foreach(contact => res = res.copy(contact = Some(contact)))
    repeatFlag.get.foreach(repeat => res = res.copy(repeat = repeat))
    seedFlag.get.foreach(seed => res = res.copy(seed = seed))
    labelsFlag.get.foreach { str =>
      val newLabels = str.split(',').map(splitKeyValue)
      res = res.copy(labels = res.labels ++ newLabels)
    }
    resourcesFlag.get.foreach { str =>
      val newResources = str.split(',')
        .map(splitKeyValue)
        .map { case (k, v) => k -> v.toLong }
      val resources = res.resources.filterNot(newResources.contains) ++ newResources
      res = res.copy(resources = resources)
    }
    if (residue.nonEmpty) {
      val newParams = getParams(residue)
      val params = res.params.filterNot(newParams.contains) ++ newParams
      res = res.copy(params = params)
    }
    res
  }

  private def splitKeyValue(str: String): (String, String) = {
    val pos = str.indexOf('=')
    if (pos > -1) {
      str.take(pos) -> str.drop(pos + 1)
    } else {
      str.take(pos) -> "true"
    }
  }

  private def getParams(residue: Seq[String]): Seq[AttrValue] = {
    residue.map { str =>
      val (k, v) = splitKeyValue(str)
      AttrValue(k, Value.String(v))
    }
  }
}