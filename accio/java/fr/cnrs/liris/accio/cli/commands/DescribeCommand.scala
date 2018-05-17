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

import com.twitter.util._
import fr.cnrs.liris.accio.cli.controller._
import fr.cnrs.liris.accio.cli.event.{Event, Reporter}

final class DescribeCommand extends Command with ClientCommand {
  override def name = "describe"

  override def help = "Display details about a specific resource."

  override def allowResidue = true

  override def execute(residue: Seq[String], env: CommandEnvironment): Future[ExitCode] = {
    if (residue.size < 2) {
      env.reporter.handle(Event.error("You must specify a resource type and identifier.\n" +
        "Valid resource types are: job, task, operator."))
      return Future.value(ExitCode.CommandLineError)
    }
    val controller: DescribeController[_] = residue.head match {
      case "job" | "jobs" => new DescribeJobController
      case "task" | "tasks" => new DescribeTaskController
      case "operator" | "operators" | "op" | "ops" => new DescribeOperatorController
      case _ =>
        env.reporter.handle(Event.error(s"Invalid resource type: ${residue.head}.\n" +
          s"Valid resource types are: workflow, run, node, operator."))
        return Future.value(ExitCode.CommandLineError)
    }
    describe(controller, env.reporter, residue.tail)
  }

  private def describe[Res](controller: DescribeController[Res], reporter: Reporter, residue: Seq[String]) = {
    val fs = residue.map { id =>
      controller
        .retrieve(id, client)
        .map { resp =>
          controller.print(reporter, resp)
          ExitCode.Success
        }
    }
    Future.collect(fs).map(ExitCode.select)
  }
}