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
import fr.cnrs.liris.accio.agent.GetClusterRequest
import fr.cnrs.liris.accio.core.util.Version
import fr.cnrs.liris.common.cli.{Cmd, Command, ExitCode, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class VersionCommandFlags(
  @Flag(name = "client", help = "Show only version of the client")
  client: Boolean = false)

@Cmd(
  name = "version",
  help = "Display client build information.",
  flags = Array(classOf[CommonCommandFlags], classOf[VersionCommandFlags]))
class VersionCommand @Inject()(clientProvider: ClusterClientProvider) extends Command {
  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (!flags.as[VersionCommandFlags].client) {
      val client = clientProvider(flags.as[CommonCommandFlags].cluster)
      val f = client.getCluster(GetClusterRequest())
      Await.result(f.liftToTry) match {
        case Return(resp) =>
          out.writeln(s"Server version: ${resp.version}")
          out.writeln(s"Client version: ${Version.Current.toString}")
          ExitCode.Success
        case Throw(e) =>
          out.writeln(s"<error>[ERROR]</error> Server error: ${e.getMessage}")
          ExitCode.InternalError
      }
    } else {
      out.writeln(s"Client version: ${Version.Current.toString}")
      ExitCode.Success
    }
  }
}