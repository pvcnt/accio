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
import fr.cnrs.liris.accio.agent.KillRunRequest
import fr.cnrs.liris.accio.client.event.{Event, Reporter}
import fr.cnrs.liris.accio.client.runtime.{Cmd, ExitCode}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.flags.FlagsProvider

@Cmd(
  name = "kill",
  flags = Array(classOf[ClusterFlags]),
  help = "Stop an active run.",
  allowResidue = true)
class KillCommand @Inject()(clientProvider: ClusterClientProvider) extends AbstractCommand(clientProvider) {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 1) {
      out.handle(Event.error("<error>[ERROR]</error> You must provide a single run identifier."))
      return ExitCode.CommandLineError
    }
    val client = createClient(flags)
    val req = KillRunRequest(RunId(flags.residue.head))
    Await.result(client.killRun(req).liftToTry) match {
      case Return(_) =>
        out.handle(Event.info(s"Killed run ${flags.residue.head}"))
        ExitCode.Success
      case Throw(UnknownRunException()) =>
        out.handle(Event.error(s"Unknown run: ${flags.residue.head}"))
        ExitCode.CommandLineError
      case Throw(e) =>
        out.error("Server error", e)
        ExitCode.InternalError
    }
  }
}