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

import com.twitter.util.{Future, Time}
import com.twitter.util.logging.Logging
import fr.cnrs.liris.lumos.domain.Event
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.server.{LumosService, PushEventRequest}

import scala.collection.mutable

final class LumosServiceEventTransport(client: LumosService.MethodPerEndpoint) extends EventTransport with Logging {
  private[this] val queue = mutable.ListBuffer.empty[Event]
  private[this] var lastSequence = -1L

  override def name = "LumosService"

  override def sendEvent(event: Event): Future[Unit] = synchronized {
    if (queue.isEmpty && lastSequence == event.sequence - 1) {
      lastSequence = event.sequence
      pushEvent(event)
    } else {
      val idx = queue.indexWhere(_.sequence > event.sequence)
      if (idx > -1) {
        queue.insert(idx, event)
      } else {
        queue.append(event)
      }
      val sequences = queue.map(_.sequence).sliding(2).takeWhile(vs => vs.last == vs.head + 1).flatten.toSet
      if (sequences.nonEmpty && lastSequence == sequences.min - 1) {
        val f = Future.join(queue.take(sequences.size).map(pushEvent))
        lastSequence = sequences.max
        queue.remove(0, sequences.size)
        f
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
