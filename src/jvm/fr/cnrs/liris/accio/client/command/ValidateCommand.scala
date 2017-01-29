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

import java.nio.ByteBuffer
import java.nio.file.{Files, Path}

import com.twitter.util.{Await, Return, Throw}
import fr.cnrs.liris.accio.agent.{AgentService, ParseRunRequest, ParseWorkflowRequest}
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.JavaConverters._

case class ValidateFlags(
  @Flag(name = "keep_going", help = "Whether to continue validating other files once an error occurred")
  keepGoing: Boolean = true)

@Cmd(
  name = "validate",
  help = "Validate the syntax of Accio configuration files.",
  flags = Array(classOf[ValidateFlags]),
  allowResidue = true)
class ValidateCommand(clientFactory: AgentClientFactory) extends Command {
  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must specify some files to validate as argument.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[ValidateFlags]
      var valid = true
      var i = 0
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      while (i < flags.residue.size) {
        if (validate(flags.residue(i), client, out)) {
          i += 1
        } else {
          valid = false
          i = if (opts.keepGoing) i + 1 else flags.residue.size
        }
      }
      if (valid) {
        ExitCode.Success
      } else {
        ExitCode.ValidateFailure
      }
    }
  }

  private def validate(uri: String, client: AgentService.FinagledClient, out: Reporter): Boolean = {
    val path = FileUtils.expandPath(uri)
    if (!path.toFile.exists || !path.toFile.canRead) {
      out.writeln(s"<error>[ERROR]</error> Cannot read file: ${path.toAbsolutePath}")
      false
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      if (content.contains("\"workflow\"")) {
        validateRun(content, path, client, out)
      } else {
        validateWorkflow(content, path, client, out)
      }
    }
  }

  private def validateRun(content: String, path: Path, client: AgentService.FinagledClient, out: Reporter) = {
    val req = ParseRunRequest(content, Map.empty, Some(path.getFileName.toString))
    Await.result(client.parseRun(req).liftToTry) match {
      case Return(resp) =>
        resp.warnings.foreach { warning =>
          out.writeln(s"<comment>[WARN]</comment> $warning")
        }
        resp.errors.foreach { error =>
          out.writeln(s"<error>[ERROR]</error> $error")
        }
        if (resp.run.isDefined) {
          out.writeln(s"<info>[OK]</info> Validated file: ${path.toAbsolutePath}")
          true
        } else {
          false
        }
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        false
    }
  }

  private def validateWorkflow(content: String, path: Path, client: AgentService.FinagledClient, out: Reporter) = {
    val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
    Await.result(client.parseWorkflow(req).liftToTry) match {
      case Return(resp) =>
        resp.warnings.foreach { warning =>
          out.writeln(s"<comment>[WARN]</comment> $warning")
        }
        resp.errors.foreach { error =>
          out.writeln(s"<error>[ERROR]</error> $error")
        }
        if (resp.workflow.isDefined) {
          out.writeln(s"<info>[OK]</info> Validated file: ${path.toAbsolutePath}")
          true
        } else {
          false
        }
      case Throw(e) =>
        out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        false
    }
  }
}