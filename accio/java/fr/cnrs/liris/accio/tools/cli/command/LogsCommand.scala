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

package fr.cnrs.liris.accio.tools.cli.command

import com.google.inject.Inject
import fr.cnrs.liris.accio.agent.ListLogsRequest
import fr.cnrs.liris.accio.api.thrift.RunId
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}
import fr.cnrs.liris.common.flags.{Flag, FlagsProvider}

case class LogsCommandFlags(
  @Flag(
    name = "stderr",
    help = "Include stderr logs (instead of stdout)")
  stderr: Boolean = false,
  @Flag(
    name = "n",
    help = "Maximum number of log lines (taken from the end of the log)")
  n: Option[Int])

@Cmd(
  name = "logs",
  flags = Array(classOf[LogsCommandFlags], classOf[ClusterFlags]),
  help = "Retrieve logs of a run.",
  allowResidue = true)
class LogsCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.size != 2) {
      out.handle(Event.error("You must provide a run identifier and a node name as arguments."))
      return ExitCode.CommandLineError
    }
    val req = createRequest(flags.residue, flags.as[LogsCommandFlags])
    val client = createClient(flags)
    handleResponse(client.listLogs(req), out) { resp =>
      resp.results.foreach(line => out.outErr.printOutLn(line))
      ExitCode.Success
    }
  }

  private def createRequest(residue: Seq[String], opts: LogsCommandFlags) = {
    val classifier = if (opts.stderr) "stderr" else "stdout"
    ListLogsRequest(RunId(residue.head), residue.last, classifier, tail = opts.n)
  }
}