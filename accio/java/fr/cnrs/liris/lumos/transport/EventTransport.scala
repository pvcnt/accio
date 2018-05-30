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

import com.twitter.util.{Closable, Future}
import fr.cnrs.liris.lumos.domain.Event

/**
 * Transport for the Lumos events. All implementations should be thread-safe.
 */
trait EventTransport extends Closable {
  /**
   * Return the name of this transport, as displayed to the user.
   */
  def name: String

  /**
   * Write an event to an endpoint.
   *
   * Implementations should not contain any blocking call and return quickly, possibly before the
   * event is actually written. This method should never throw any exception.
   *
   * @param event Event to write.
   */
  def sendEvent(event: Event): Unit
}