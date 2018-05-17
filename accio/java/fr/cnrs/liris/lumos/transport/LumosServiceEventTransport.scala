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
import fr.cnrs.liris.lumos.domain.Event
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.server.{LumosService, PushEventRequest}

import scala.collection.mutable

/**
 * Transport pushing events to a Lumos server.
 *
 * @param client Lumos client.
 */
final class LumosServiceEventTransport(client: LumosService.MethodPerEndpoint)
  extends EventTransport with Logging {

  // TODO: clean that structure periodically to avoid it growing in case of stale events.
  private[this] val states = mutable.Map.empty[String, State]

  private class State(val queue: mutable.ListBuffer[Event], var lastSequence: Long)

  override def name = "LumosService"

  override def sendEvent(event: Event): Future[Unit] = synchronized {
    // All the complexity here lies in the fact that we want to enforce a correct order before
    // sending events, otherwise they may be rejected by the server. What we do is that we keep
    // a local buffer of events until we have a complete sequence, in which case we dequeue the
    // latter. We then have to send the events one by one (instead of in parallel), to ensure that
    // the previous event is indeed committed before sending the next one.

    // I am not completely convinced that this code should belongs to the client side. Maybe it
    // could be moved to the client-side, this alleviating clients of this concern.
    val state = states.getOrElseUpdate(event.parent, new State(mutable.ListBuffer.empty[Event], -1))
    if (state.queue.isEmpty && state.lastSequence == event.sequence - 1) {
      state.lastSequence = event.sequence
      pushEvent(event)
    } else {
      val idx = state.queue.indexWhere(_.sequence > event.sequence)
      if (idx > -1) {
        state.queue.insert(idx, event)
      } else {
        state.queue.append(event)
      }
      val sequences = state.queue.map(_.sequence).sliding(2).takeWhile(vs => vs.last == vs.head + 1).flatten.toSet
      // TODO: return something in case an event was not accepted by the remote server.
      if (sequences.nonEmpty && state.lastSequence == sequences.min - 1) {
        var continue = true
        var idx = 0
        Future.whileDo(continue && idx < sequences.size) {
          pushEvent(state.queue.remove(0))
            .onFailure { _ => continue = false }
            .onSuccess { _ => idx += 1 }
        }.ensure {
          state.lastSequence += idx
        }
      } else {
        Future.Done
      }
    }
  }

  override def close(deadline: Time): Future[Unit] = client.asClosable.close(deadline)

  private def pushEvent(event: Event): Future[Unit] = {
    client.pushEvent(PushEventRequest(ThriftAdapter.toThrift(event))).unit
  }
}
