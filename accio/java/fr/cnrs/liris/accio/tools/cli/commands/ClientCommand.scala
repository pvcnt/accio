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

import com.twitter.scrooge.ThriftException
import com.twitter.util.{Await, Future, Return, Throw}
import fr.cnrs.liris.accio.agent.AgentService$FinagleClient
import fr.cnrs.liris.accio.api.thrift.{InvalidSpecException, InvalidSpecMessage, UnknownRunException}
import fr.cnrs.liris.accio.tools.cli.config.ConfigParser
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventKind, Reporter}

private[commands] trait ClientCommand {
  this: Command =>

  private[this] val clusterFlag = flag[String]("cluster", "Name of the cluster to use")
  private[this] val clientProvider = new ClusterClientProvider(ConfigParser.default)

  protected final def client: AgentService$FinagleClient = {
    clusterFlag.get.map(clientProvider.apply).getOrElse(clientProvider.default)
  }

  protected final def respond[T](f: Future[T], reporter: Reporter)(fn: T => ExitCode): ExitCode = {
    Await.result(f.liftToTry) match {
      case Return(resp) => fn(resp)
      case Throw(e: InvalidSpecException) =>
        printErrors(e.warnings, reporter, EventKind.Warning)
        printErrors(e.errors, reporter, EventKind.Error)
        ExitCode.DefinitionError
      case Throw(e: UnknownRunException) =>
        reporter.handle(Event.error(s"Unknown run: ${e.id.value}"))
        ExitCode.CommandLineError
      case Throw(e: ThriftException) =>
        reporter.handle(Event.warn(e.getMessage))
        ExitCode.RuntimeError
      case Throw(e) =>
        reporter.handle(Event.error(s"Server error: ${e.getMessage}"))
        ExitCode.InternalError
    }
  }

  protected def printErrors(errors: Seq[InvalidSpecMessage], out: Reporter, eventKind: EventKind): Unit = {
    errors.foreach { error =>
      val explanation = error.message + error.path.map(path => s" (at $path)").getOrElse("")
      out.handle(Event(eventKind, explanation))
    }
  }
}