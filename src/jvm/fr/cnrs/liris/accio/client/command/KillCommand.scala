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
import fr.cnrs.liris.accio.agent.KillRunRequest
import fr.cnrs.liris.accio.runtime.event.{Event, Reporter}
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.flags.FlagsProvider

@Cmd(
  name = "kill",
  flags = Array(classOf[ClusterFlags]),
  help = "Cancel an active run.",
  allowResidue = true)
class KillCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      reporter.handle(Event.error("You must provide at least one run identifier."))
      return ExitCode.CommandLineError
    }
    val client = createClient(flags)
    val outcomes = flags.residue.map { id =>
      val req = KillRunRequest(RunId(id))
      handleResponse(client.killRun(req), reporter) { _ =>
        reporter.handle(Event.info(s"Killed run ${flags.residue.head}"))
        ExitCode.Success
      }
    }
    ExitCode.select(outcomes)
  }
}