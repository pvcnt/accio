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
import fr.cnrs.liris.accio.client.client.ClusterClientProvider
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}
import fr.cnrs.liris.common.util.TimeUtils

case class RmCommandFlags(
  @Flag(name = "quiet", help = "Quiet output")
  quiet: Boolean = false)

@Cmd(
  name = "rm",
  flags = Array(classOf[CommonCommandFlags], classOf[RmCommandFlags]),
  help = "Delete a run.",
  allowResidue = true)
class RmCommand @Inject()(clientProvider: ClusterClientProvider) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 1) {
      out.writeln("<error>[ERROR]</error> You must provide a single run identifier.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[RmCommandFlags]
      val elapsed = Stopwatch.start()
      val client = clientProvider(flags.as[CommonCommandFlags].cluster)
      val req = DeleteRunRequest(RunId(flags.residue.head))
      val exitCode = Await.result(client.deleteRun(req).liftToTry) match {
        case Return(_) =>
          if (!opts.quiet) {
            out.writeln(s"<info>[OK]</info> Deleted run ${flags.residue.head}")
          }
          ExitCode.Success
        case Throw(UnknownRunException()) =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Unknown run ${flags.residue.head}")
          }
          ExitCode.CommandLineError
        case Throw(e) =>
          if (!opts.quiet) {
            out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          }
          ExitCode.InternalError
      }
      if (!opts.quiet) {
        out.writeln(s"<info>[OK]</info> Done in ${TimeUtils.prettyTime(elapsed())}.")
      }
      exitCode
    }
  }
}