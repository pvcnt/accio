/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.lumos.cli

import com.twitter.util.Future
import fr.cnrs.liris.infra.cli.app.{Environment, ExitCode}
import fr.cnrs.liris.lumos.version.Version

final class VersionCommand extends LumosCommand {
  private[this] val onlyClient = flag("client", false, "Show only version of the client (no server needed)")

  override def name = "version"

  override def help = "Display client and server version information."

  override def execute(residue: Seq[String], env: Environment): Future[ExitCode] = {
    if (onlyClient()) {
      env.reporter.outErr.printOutLn(s"Client version: ${Version.Current}")
      Future.value(ExitCode.Success)
    } else {
      val client = createLumosClient(env)
      client.getInfo().map { resp =>
        env.reporter.outErr.printOutLn(s"Server version: ${resp.version}")
        env.reporter.outErr.printOutLn(s"Client version: ${Version.Current}")
        ExitCode.Success
      }
    }
  }
}