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

import com.google.inject.Inject
import com.twitter.util.{Await, Return, Stopwatch, Throw}
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.client.service.{AgentClientFactory, ParsingException, WorkflowDefFactory}
import fr.cnrs.liris.accio.core.domain.{InvalidWorkflowDefException, Utils}
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.accio.core.service.handler.PushWorkflowRequest
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.TimeUtils

case class PushCommandFlags(
  @Flag(name = "q", help = "Print only identifiers")
  quiet: Boolean = false)

@Cmd(
  name = "push",
  flags = Array(classOf[PushCommandFlags], classOf[AccioAgentFlags]),
  help = "Push a workflow.",
  allowResidue = true)
class PushCommand @Inject()(clientFactory: AgentClientFactory, factory: WorkflowDefFactory)
  extends Command with StrictLogging {

  def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must provide exactly at least one workflow definition file.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[PushCommandFlags]
      val addr = flags.as[AccioAgentFlags].addr
      val elapsed = Stopwatch.start()
      val outcomes = flags.residue.map(uri => tryPush(uri, opts, addr, out))
      if (!opts.quiet) {
        out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      }
      if (outcomes.forall(_ == true)) ExitCode.Success else ExitCode.InternalError
    }
  }

  private def tryPush(uri: String, opts: PushCommandFlags, addr: String, out: Reporter): Boolean = {
    val defn = try {
      factory.create(uri)
    } catch {
      case e: ParsingException =>
        if (!opts.quiet) {
          out.writeln(s"<error>[ERROR]</error> Workflow definition parse error: ${e.getMessage}")
        }
        return false
      case e: InvalidWorkflowDefException =>
        if (!opts.quiet) {
          out.writeln(s"<error>[ERROR]</error> Workflow definition error: ${e.getMessage}")
        }
        return false
    }
    val req = PushWorkflowRequest(defn, Utils.DefaultUser)
    val client = clientFactory.create(addr)
    Await.result(client.pushWorkflow(req).liftToTry) match {
      case Return(_) =>
        if (!opts.quiet) {
          out.writeln(s"<info>[OK]</info> Pushed workflow: ${defn.id.value}")
        } else {
          out.writeln(defn.id.value)
        }
        true
      case Throw(e) =>
        if (!opts.quiet) {
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
        }
        false
    }
  }
}