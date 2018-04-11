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

package fr.cnrs.liris.accio.tools.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.tools.cli.controller._
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}
import fr.cnrs.liris.util.StringUtils.explode

final class GetCommand extends Command with ClientCommand {
  private[this] val allFlag = flag("all", false, "Show all resources, including those inactive")
  private[this] val tagsFlag = flag[String]("tags", "Show only resources including one of given tags (comma-separated)")
  private[this] val ownerFlag = flag[String]("owner", "Show only resources belonging to a given owner")
  private[this] val limitFlag = flag[Int]("n", "Limit the number of shown resources")

  override def name = "get"

  override def help = "Display a list of resources."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.isEmpty) {
      env.reporter.handle(Event.error("You must specify a resource type.\n" +
        "Valid resource types are: workflow, run, operator"))
      return Future.value(ExitCode.CommandLineError)
    }
    val controller: GetController[_] = residue.head match {
      case "workflow" | "workflows" => new GetWorkflowController
      case "run" | "runs" => new GetRunController
      case "operator" | "operators" | "op" | "ops" => new GetOperatorController
      case _ =>
        env.reporter.handle(Event.error(s"Invalid resource type: ${residue.head}.\n" +
          s"Valid resource types are: workflow, run, operator."))
        return Future.value(ExitCode.CommandLineError)
    }
    list(controller, env.reporter)
  }

  private def list[Res](controller: GetController[Res], reporter: Reporter) = {
    val query = GetQuery(
      all = allFlag(),
      owner = ownerFlag.get,
      tags = explode(tagsFlag.get, ","),
      limit = limitFlag.get)
    controller.retrieve(query, client).map { resp =>
      controller.print(reporter, resp)
      ExitCode.Success
    }
  }
}