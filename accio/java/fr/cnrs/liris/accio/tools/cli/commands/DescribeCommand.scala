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

import com.twitter.util._
import fr.cnrs.liris.accio.tools.cli.controller._
import fr.cnrs.liris.accio.tools.cli.event.{Event, Reporter}

final class DescribeCommand extends Command with ClientCommand {
  override def name = "describe"

  override def help = "Display details about a specific resource."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): ExitCode = {
    if (residue.size < 2) {
      env.reporter.handle(Event.error("You must specify a resource type and identifier"))
      return ExitCode.CommandLineError
    }
    val maybeController: Option[DescribeController[_]] = residue.head match {
      case "run" | "runs" => Some(new DescribeRunController)
      case "node" | "nodes" => Some(new DescribeNodeController)
      case "workflow" | "workflows" => Some(new DescribeWorkflowController)
      case "operator" | "operators" | "op" | "ops" => Some(new DescribeOperatorController)
      case _ => None
    }
    maybeController match {
      case None =>
        env.reporter.handle(Event.error(s"Invalid resource type: ${residue.head}"))
        ExitCode.CommandLineError
      case Some(controller) => execute(controller, env.reporter, residue.last)
    }
  }

  private def execute[Res](controller: DescribeController[Res], reporter: Reporter, id: String) = {

    val f = controller.retrieve(id, client).liftToTry
    Await.result(f) match {
      case Return(resp) =>
        controller.print(reporter, resp)
        ExitCode.Success
      case Throw(NoResultException()) =>
        reporter.handle(Event.error(s"No such resource: $id"))
        ExitCode.CommandLineError
      case Throw(e) =>
        reporter.error(s"Server error", e)
        ExitCode.InternalError
    }
  }
}