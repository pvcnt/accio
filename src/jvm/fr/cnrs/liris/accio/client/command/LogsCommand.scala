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
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.infra.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.FlagsProvider

case class LogsFlags()

@Cmd(
  name = "inspect",
  flags = Array(classOf[LogsFlags]),
  help = "Display logs.",
  allowResidue = true)
class LogsCommand @Inject()(client: AgentService.FinagledClient) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 2) {
      out.writeln("<error>[ERROR]</error> You must provide exactly a run identifier and a node name.")
      ExitCode.CommandLineError
    } else {
      val opts = flags.as[LogsFlags]
      /*val f = client.getRun(GetLogsRequest(RunId(flags.residue.head))).liftToTry
      Await.result(f) match {
        case Return(resp) =>

        case Throw(e) =>
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          ExitCode.InternalError
      }*/
      ExitCode.Success
    }
  }
}
