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

package fr.cnrs.liris.accio.runtime.event

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

trait EventHandler {
  def handle(event: Event): Unit
}

final class CollectorEventHandler(eventMask: Set[EventKind] = EventKind.values)
  extends EventHandler with Iterable[Event] {

  private[this] val events = mutable.ListBuffer.empty[Event]

  override def handle(event: Event): Unit = synchronized {
    if (eventMask.contains(event.kind)) {
      events ++= Seq(event)
    }
  }

  override def iterator: Iterator[Event] = events.iterator
}

final class FilterEventHandler(eventMask: Set[EventKind], handler: EventHandler) extends EventHandler {
  override def handle(event: Event): Unit = {
    if (eventMask.contains(event.kind)) {
      handler.handle(event)
    }
  }
}

final class StoreEventHandler extends EventHandler {
  private[this] val _events = mutable.ListBuffer.empty[Event]

  override def handle(event: Event): Unit = synchronized {
    _events ++ Seq(event)
  }

  def replay(handler: EventHandler): Unit = synchronized {
    _events.foreach(handler.handle)
    _events.clear()
  }

  def clear(): Unit = synchronized {
    _events.clear()
  }
}

final class SensorEventHandler(mask: Set[EventKind]) extends EventHandler {
  private[this] val _count = new AtomicInteger(0)

  override def handle(event: Event): Unit = {
    if (mask.contains(event.kind)) {
      _count.incrementAndGet()
    }
  }

  def count: Int = _count.get
}