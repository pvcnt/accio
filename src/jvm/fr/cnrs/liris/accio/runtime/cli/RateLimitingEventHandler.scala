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

package fr.cnrs.liris.accio.runtime.cli

import com.twitter.util.{Duration, Time}
import fr.cnrs.liris.accio.runtime.event.{Event, EventHandler, EventKind}

/**
 * Creates a new Event handler that rate limits the events of type PROGRESS
 * to one per event "rateLimitation" seconds.  Events that arrive too quickly are dropped;
 * all others are are forwarded to the handler "delegateTo".
 *
 * @param handler   The event handler that ultimately handles the events
 * @param rateLimit The duration between events that will be forwarded to the underlying handler.
 */
private[cli] final class RateLimitingEventHandler(handler: EventHandler, rateLimit: Duration) extends EventHandler {
  require(rateLimit > Duration.Zero, "Rate limit must be strictly positive")
  private[this] var lastEvent = Time.Undefined

  override def handle(event: Event): Unit = {
    event.kind match {
      case EventKind.Progress | EventKind.Start | EventKind.Finish =>
        val currentTime = Time.now
        if (lastEvent == Time.Undefined || lastEvent + rateLimit <= currentTime) {
          lastEvent = currentTime
          handler.handle(event)
        }
      case _ => handler.handle(event)
    }
  }
}