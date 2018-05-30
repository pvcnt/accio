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

package fr.cnrs.liris.lumos.transport

import java.util.concurrent.atomic.AtomicInteger

import com.google.inject.{Inject, Singleton}
import com.twitter.util.{Future, Time}
import fr.cnrs.liris.lumos.domain.Event

/**
 * An event transport that forwards events to underlying transports.
 *
 * @param transports A set of transports to forward the events to.
 */
@Singleton
final class EventTransportMultiplexer @Inject()(transports: Set[EventTransport])
  extends EventTransport {

  private[this] val errors = new AtomicInteger(0)

  def hasErrors: Boolean = errors.get > 0

  override def name = s"Multiplexer[${transports.map(_.name).mkString(",")}]"

  override def sendEvent(event: Event): Unit = transports.foreach(_.sendEvent(event))

  override def close(deadline: Time): Future[Unit] = {
    Future.join(transports.map(_.close(deadline)).toSeq)
  }
}
