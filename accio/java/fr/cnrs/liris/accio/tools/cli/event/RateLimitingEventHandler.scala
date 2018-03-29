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

package fr.cnrs.liris.accio.tools.cli.event

import com.twitter.util.{Duration, Time}

/**
 * Creates a new Event handler that rate limits the events of type PROGRESS
 * to one per event "rateLimitation" seconds.  Events that arrive too quickly are dropped;
 * all others are are forwarded to the handler "delegateTo".
 *
 * @param handler   The event handler that ultimately handles the events
 * @param rateLimit The duration between events that will be forwarded to the underlying handler.
 */
final class RateLimitingEventHandler(handler: EventHandler, rateLimit: Duration) extends EventHandler {
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