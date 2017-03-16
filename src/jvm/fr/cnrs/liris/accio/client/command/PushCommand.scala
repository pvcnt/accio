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

import java.nio.file.Files

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Stopwatch, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, ParseWorkflowRequest, PushWorkflowRequest}
import fr.cnrs.liris.accio.client.client.ClusterClientProvider
import fr.cnrs.liris.accio.core.domain.{InvalidSpecException, Utils, WorkflowSpec}
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, TimeUtils}

import scala.collection.JavaConverters._

case class PushCommandFlags(
  @Flag(name = "quiet", help = "Print only identifiers")
  quiet: Boolean = false)

@Cmd(
  name = "push",
  flags = Array(classOf[PushCommandFlags], classOf[CommonCommandFlags]),
  help = "Push a workflow.",
  allowResidue = true)
class PushCommand @Inject()(clientProvider: ClusterClientProvider)
  extends Command with DefinitionFileCommand with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must provide exactly at least one workflow definition file.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[PushCommandFlags]
      val elapsed = Stopwatch.start()
      val client = clientProvider(flags.as[CommonCommandFlags].cluster)
      val outcomes = flags.residue.map(uri => parseAndPush(uri, opts, client, out))
      if (!opts.quiet) {
        out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      }
      ExitCode.select(outcomes)
    }
  }

  private def parseAndPush(uri: String, opts: PushCommandFlags, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      out.writeln(s"<error>[ERROR]</error> Cannot read workflow definition file: ${path.toAbsolutePath}")
      ExitCode.DefinitionError
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      val req = ParseWorkflowRequest(content, Some(path.getFileName.toString))
      Await.result(client.parseWorkflow(req).liftToTry) match {
        case Return(resp) =>
          if (!opts.quiet) {
            printWarnings(resp.warnings, out)
            printErrors(resp.errors, out)
          }
          resp.workflow match {
            case Some(spec) => push(spec, opts, client, out)
            case None =>
              if (!opts.quiet) {
                out.writeln("<error>[ERROR]</error> Some errors where found in the workflow definition")
              }
              ExitCode.DefinitionError
          }
        case Throw(e) =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          }
          ExitCode.InternalError
      }
    }
  }

  private def push(spec: WorkflowSpec, opts: PushCommandFlags, client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val req = PushWorkflowRequest(spec, Utils.DefaultUser)
    Await.result(client.pushWorkflow(req).liftToTry) match {
      case Return(_) =>
        if (!opts.quiet) {
          out.writeln(s"<info>[OK]</info> Pushed workflow: ${spec.id.value}")
        } else {
          out.writeln(spec.id.value)
        }
        ExitCode.Success
      case Throw(e: InvalidSpecException) =>
        printWarnings(e.warnings, out)
        printErrors(e.errors, out)
        ExitCode.DefinitionError
      case Throw(e) =>
        if (!opts.quiet) {
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        }
        ExitCode.InternalError
    }
  }
}