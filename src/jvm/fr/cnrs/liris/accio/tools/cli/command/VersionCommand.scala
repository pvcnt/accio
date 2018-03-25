/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.tools.cli.command

import com.google.inject.Inject
import fr.cnrs.liris.accio.agent.GetClusterRequest
import fr.cnrs.liris.accio.runtime.event.Reporter
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.util.Version
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class VersionCommandFlags(
  @Flag(
    name = "client",
    help = "Show only version of the client (no server needed)")
  client: Boolean = false)

@Cmd(
  name = "version",
  help = "Display client and server version information.",
  flags = Array(classOf[ClusterFlags], classOf[VersionCommandFlags]))
class VersionCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  override def execute(flags: FlagsProvider, reporter: Reporter): ExitCode = {
    if (!flags.as[VersionCommandFlags].client) {
      // Print client and server version.
      val client = createClient(flags)
      handleResponse(client.getCluster(GetClusterRequest()), reporter) { resp =>
        reporter.outErr.printOutLn(s"Server version: ${resp.version}")
        reporter.outErr.printOutLn(s"Client version: ${Version.Current.toString}")
        ExitCode.Success
      }
    } else {
      // Print only client version.
      reporter.outErr.printOutLn(s"Client version: ${Version.Current.toString}")
      ExitCode.Success
    }
  }
}