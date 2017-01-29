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
import java.nio.file.Files

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Stopwatch, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.agent.{AgentService, ParseWorkflowRequest, PushWorkflowRequest}
import fr.cnrs.liris.accio.core.domain.{InvalidSpecException, InvalidSpecMessage, Utils}
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.{FileUtils, TimeUtils}

import scala.collection.JavaConverters._

case class PushCommandFlags(
  @Flag(name = "q", help = "Print only identifiers")
  quiet: Boolean = false)

@Cmd(
  name = "push",
  flags = Array(classOf[PushCommandFlags], classOf[AccioAgentFlags]),
  help = "Push a workflow.",
  allowResidue = true)
class PushCommand @Inject()(clientFactory: AgentClientFactory)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must provide exactly at least one workflow definition file.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[PushCommandFlags]
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      val elapsed = Stopwatch.start()
      val outcomes = flags.residue.map(uri => tryPush(uri, opts, client, out))
      if (!opts.quiet) {
        out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      }
      if (outcomes.forall(_ == true)) ExitCode.Success else ExitCode.InternalError
    }
  }

  private def tryPush(uri: String, opts: PushCommandFlags, client: AgentService.FinagledClient, out: Reporter): Boolean = {
    val path = FileUtils.expandPath(uri)
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      out.writeln(s"<error>[ERROR]</error> Cannot read workflow definition file: ${path.toAbsolutePath}")
      false
    } else {
      val content = Files.readAllLines(path).asScala.mkString
      val parseReq = ParseWorkflowRequest(content, Some(path.getFileName.toString))
      Await.result(client.parseWorkflow(parseReq).liftToTry) match {
        case Return(parseResp) =>
          if (!opts.quiet) {
            parseResp.warnings.foreach { warning =>
              out.writeln(s"<comment>[WARN]</comment> $warning")
            }
          }
          parseResp.workflow match {
            case Some(spec) =>
              val pushReq = PushWorkflowRequest(spec, Utils.DefaultUser)
              Await.result(client.pushWorkflow(pushReq).liftToTry) match {
                case Return(_) =>
                  if (!opts.quiet) {
                    out.writeln(s"<info>[OK]</info> Pushed workflow: ${spec.id.value}")
                  } else {
                    out.writeln(spec.id.value)
                  }
                  true
                case Throw(e: InvalidSpecException) =>
                  e.warnings.foreach { warning =>
                    out.writeln(s"<comment>[WARN]</comment> $warning")
                  }
                  e.errors.foreach { error =>
                    out.writeln(s"<error>[ERROR]</error> $error")
                  }
                  false
                case Throw(e) =>
                  if (!opts.quiet) {
                    out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
                  }
                  false
              }
            case None =>
              if (!opts.quiet) {
                out.writeln("<error>[ERROR]</error> Some errors where found in the workflow definition")
                parseResp.errors.foreach { error =>
                  out.writeln(s"<error>[ERROR]</error>   - $error")
                }
              }
              false
          }
        case Throw(e) =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          }
          false
      }
    }
  }

}