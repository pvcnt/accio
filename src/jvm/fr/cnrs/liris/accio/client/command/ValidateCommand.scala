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

package fr.cnrs.liris.accio.client.command

import java.nio.file.{Files, Path}

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Stopwatch, Throw}
import fr.cnrs.liris.accio.agent.{AgentService, ParseRunRequest, ParseWorkflowRequest}
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.FlagsProvider
import fr.cnrs.liris.common.util.{FileUtils, TimeUtils}

import scala.collection.JavaConverters._

@Cmd(
  name = "validate",
  help = "Validate the syntax of Accio definition files.",
  flags = Array(classOf[AccioAgentFlags]),
  allowResidue = true)
class ValidateCommand @Inject()(clientFactory: AgentClientFactory) extends Command with DefinitionFileCommand {
  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must specify some files to validate as argument.")
      ExitCode.CommandLineError
    } else {
      val elapsed = Stopwatch.start()
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      val outcomes = flags.residue.map(uri => validate(uri, client, out))
      out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      ExitCode.select(outcomes)
    }
  }

  private def validate(uri: String, client: AgentService.FinagledClient, out: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    if (!path.toFile.exists || !path.toFile.canRead) {
      out.writeln(s"<error>[ERROR]</error> Cannot read file: ${path.toAbsolutePath}")
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      if (content.contains("\"workflow\"")) {
        validateRun(content, path, client, out)
      } else {
        validateWorkflow(content, path, client, out)
      }
    }
  }

  private def validateRun(content: String, path: Path, client: AgentService.FinagledClient, out: Reporter): ExitCode = {
    val req = ParseRunRequest(content, Map.empty, Some(path.getFileName.toString))
    Await.result(client.parseRun(req).liftToTry) match {
      case Return(resp) =>
        printWarnings(resp.warnings, out)
        printErrors(resp.errors, out)
        if (resp.run.isDefined) {
          out.writeln(s"<info>[OK]</info> Validated file: ${path.toAbsolutePath}")
          ExitCode.Success
        } else {
          ExitCode.DefinitionError
        }
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        ExitCode.InternalError
    }
  }

  private def validateWorkflow(content: String, path: Path, client: AgentService.FinagledClient, out: Reporter): ExitCode = {
    val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
    Await.result(client.parseWorkflow(req).liftToTry) match {
      case Return(resp) =>
        printWarnings(resp.warnings, out)
        printErrors(resp.errors, out)
        if (resp.workflow.isDefined) {
          out.writeln(s"<info>[OK]</info> Validated file: ${path.toAbsolutePath}")
          ExitCode.Success
        } else {
          ExitCode.DefinitionError
        }
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        ExitCode.InternalError
    }
  }
}