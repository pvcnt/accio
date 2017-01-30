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
import fr.cnrs.liris.accio.agent.DeleteRunRequest
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.TimeUtils

case class RmCommandFlags(
  @Flag(name = "quiet", help = "Quiet output")
  quiet: Boolean = false)

@Cmd(
  name = "rm",
  flags = Array(classOf[AccioAgentFlags], classOf[RmCommandFlags]),
  help = "Delete runs.",
  allowResidue = true)
class RmCommand @Inject()(clientFactory: AgentClientFactory) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.writeln("<error>[ERROR]</error> You must provide some run identifiers.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[RmCommandFlags]
      val elapsed = Stopwatch.start()
      val client = clientFactory.create(flags.as[AccioAgentFlags].addr)
      val outcomes = flags.residue.map { id =>
        Await.result(client.deleteRun(DeleteRunRequest(RunId(id))).liftToTry) match {
          case Return(_) =>
            if (!opts.quiet) {
              out.writeln(s"<info>[INFO]</info> Deleted run $id")
            }
            ExitCode.Success
          case Throw(e) =>
            if (!opts.quiet) {
              out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
            }
            ExitCode.InternalError
        }
      }
      if (!opts.quiet) {
        out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      }
      ExitCode.select(outcomes)
    }
  }
}