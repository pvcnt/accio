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
import fr.cnrs.liris.accio.agent.{AgentService$FinagleClient, DeleteRunRequest}
import fr.cnrs.liris.accio.runtime.event.{Event, Reporter}
import fr.cnrs.liris.accio.runtime.cli.{Cmd, ExitCode}
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.common.flags.FlagsProvider

@Cmd(
  name = "delete",
  flags = Array(classOf[ClusterFlags]),
  help = "Delete resources.",
  allowResidue = true)
class DeleteCommand @Inject()(clientProvider: ClusterClientProvider) extends ClientCommand(clientProvider) {

  override def execute(flags: FlagsProvider, out: Reporter): ExitCode = {
    if (flags.residue.isEmpty) {
      out.handle(Event.error("You must specify identifiers of resources to delete"))
      ExitCode.CommandLineError
    }
    val client = createClient(flags)
    flags.residue.head match {
      case "run" | "runs" => deleteRuns(flags.residue.tail, client, out)
      case unknown =>
        out.handle(Event.error(s"Invalid resource type: $unknown"))
        ExitCode.CommandLineError
    }

  }

  private def deleteRuns(args: Seq[String], client: AgentService$FinagleClient, out: Reporter): ExitCode = {
    val outcomes = args.map { id =>
      val req = DeleteRunRequest(RunId(id))
      handleResponse(client.deleteRun(req), out) { _ =>
        out.handle(Event.info(s"Deleted run $id"))
        ExitCode.Success
      }
    }
    ExitCode.select(outcomes)
  }
}