/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.lumos.transport

import com.twitter.util.logging.Logging
import com.twitter.util.{Future, Time}
import fr.cnrs.liris.infra.thriftserver.ServerError
import fr.cnrs.liris.lumos.domain.Event
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.server.{LumosService, PushEventRequest}

import scala.collection.mutable

/**
 * Transport sending events to a Lumos server.
 *
 * @param client Lumos client, assumed to be managed by this transport instance.
 */
final class LumosServiceEventTransport(client: LumosService.MethodPerEndpoint)
  extends EventTransport with Logging {

  private[this] val states = mutable.Map.empty[String, State]

  private class State(val queue: mutable.Queue[Event], var active: Boolean)

  override def name = "LumosService"

  override def sendEvent(event: Event): Unit = synchronized {
    if (!states.contains(event.parent)) {
      states(event.parent) = new State(mutable.Queue.empty, active = false)
    }
    val state = states(event.parent)
    if (!state.active) {
      state.active = true
      pushEvent(event)
    } else {
      state.queue.enqueue(event)
    }
  }

  override def close(deadline: Time): Future[Unit] = synchronized {
    // TODO: wait for current upload to complete.
    client.asClosable.close(deadline).ensure(states.clear())
  }

  private def pushEvent(event: Event): Unit = {
    // TODO: handle retries.
    client
      .pushEvent(PushEventRequest(ThriftAdapter.toThrift(event)))
      .onSuccess { _ =>
        if (event.payload.isTerminal) {
          states.remove(event.parent)
        } else {
          processQueue(event.parent)
        }
      }
      .onFailure {
        case e: ServerError => logger.error(s"Error while sending event: ${format(e)}")
        case e: Throwable => logger.error(s"Internal error while sending event", e)
      }
  }

  private def processQueue(parent: String): Unit = synchronized {
    val state = states(parent)
    if (state.queue.nonEmpty) {
      pushEvent(state.queue.dequeue())
    } else {
      state.active = false
    }
  }

  private def format(e: ServerError) = {
    var sb = new StringBuilder
    e.resourceType.foreach { resourceType =>
      sb ++= resourceType
      sb ++= (if (e.resourceName.isDefined) "/" else " ")
    }
    e.resourceName.foreach { resourceName =>
      sb ++= resourceName
      sb += ' '
    }
    sb ++= e.code.name
    sb ++= ". "
    e.message.foreach { message =>
      sb ++= message
      if (!message.endsWith(".")) {
        sb += '.'
      }
    }
    e.errors.toSeq.flatten.foreach { error =>
      sb ++= s"\n - ${error.message} at ${error.field}"
    }
    sb.toString
  }
}
