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

import com.twitter.scrooge.ThriftException
import com.twitter.util.{Await, Future, Return, Throw}
import fr.cnrs.liris.accio.agent.AgentService$FinagleClient
import fr.cnrs.liris.accio.core.domain.{InvalidSpecException, InvalidSpecMessage, UnknownRunException}
import fr.cnrs.liris.accio.runtime.cli.{Command, ExitCode}
import fr.cnrs.liris.accio.runtime.event.{Event, EventKind, Reporter}
import fr.cnrs.liris.common.flags.FlagsProvider

private[command] abstract class ClientCommand(clientProvider: ClusterClientProvider) extends Command {
  protected final def createClient(flags: FlagsProvider): AgentService$FinagleClient = {
    clientProvider(flags.as[ClusterFlags].cluster)
  }

  protected final def handleResponse[T](f: Future[T], out: Reporter)(fn: T => ExitCode): ExitCode = {
    Await.result(f.liftToTry) match {
      case Return(resp) => fn(resp)
      case Throw(e: InvalidSpecException) =>
        printErrors(e.warnings, out, EventKind.Warning)
        printErrors(e.errors, out, EventKind.Error)
        ExitCode.DefinitionError
      case Throw(e: UnknownRunException) =>
        out.handle(Event.error(s"Unknown run: ${e.id.value}"))
        ExitCode.CommandLineError
      case Throw(e: ThriftException) =>
        out.handle(Event.warn(e.getMessage))
        ExitCode.RuntimeError
      case Throw(e) =>
        out.handle(Event.error(s"Server error: ${e.getMessage}"))
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