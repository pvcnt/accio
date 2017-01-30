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
import com.twitter.util.{Await, Return, Throw}
import fr.cnrs.liris.accio.agent.ListLogsRequest
import fr.cnrs.liris.accio.core.domain.RunId
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class LogsCommandFlags(
  @Flag(name = "stdout", help = "Include stdout logs")
  stdout: Boolean = true,
  @Flag(name = "stderr", help = "Include stderr logs")
  stderr: Boolean = true,
  @Flag(name = "n", help = "Maximum number of results")
  n: Option[Int])

@Cmd(
  name = "logs",
  flags = Array(classOf[LogsCommandFlags], classOf[AccioAgentFlags]),
  help = "Retrieve logs of a run.",
  allowResidue = true)
class LogsCommand @Inject()(clientFactory: AgentClientFactory) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 2) {
      out.writeln("<error>[ERROR]</error> You must provide a run identifier and a node name as arguments.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[LogsCommandFlags]
      val req = createRequest(flags.residue, opts)
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      Await.result(client.listLogs(req).liftToTry) match {
        case Return(resp) =>
          resp.results.foreach { log =>
            out.writeln(s"<comment>${log.classifier}:</comment> ${log.message}")
          }
        case Throw(e) =>
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          ExitCode.InternalError
      }
      ExitCode.Success
    }
  }

  private def createRequest(residue: Seq[String], opts: LogsCommandFlags) = {
    val classifier = if (opts.stderr && opts.stdout) None else if (opts.stderr) Some("stderr") else Some("stdout")
    ListLogsRequest(RunId(residue.head), residue.last, classifier, opts.n)
  }
}
