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

package fr.cnrs.liris.accio.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.server.ListLogsRequest
import fr.cnrs.liris.accio.cli.event.Event

final class LogsCommand extends Command with ClientCommand {
  private[this] val stderrFlag = flag("stderr", false, "Include stderr logs (instead of stdout)")
  private[this] val tailFlag = flag[Int]("n", "Maximum number of log lines (taken from the end of the log)")

  override def name = "logs"

  override def help = "Retrieve logs of a run."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.size != 2) {
      env.reporter.handle(Event.error("You must provide a run identifier and a node name as arguments."))
      return Future.value(ExitCode.CommandLineError)
    }
    val classifier = if (stderrFlag()) "stderr" else "stdout"
    val req = ListLogsRequest(residue.head, residue.last, classifier, tail = tailFlag.get)
    client.listLogs(req).map { resp =>
      resp.results.foreach(line => env.reporter.outErr.printOutLn(line))
      ExitCode.Success
    }
  }
}