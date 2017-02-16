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

package fr.cnrs.liris.privamov.core.model

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.common.geo.{Location, Point}
import org.joda.time.{Instant, ReadableInstant}

/**
 * The smallest piece of information of our model. It is a discrete event associated with a user,
 * that occurred at an instant and a specific place.
 *
 * @param user  User identifier.
 * @param point Location.
 * @param time  Timestamp.
 */
case class Event private(user: String, point: Point, time: Instant) extends Ordered[Event] {
  /**
   * Events can be compared using their timestamp.
   *
   * @param that Other event to compare with.
   * @return x s.t. x < 0 iff this < that, x == 0 iff this == that, x > 0 iff this > that.
   */
  override def compare(that: Event): Int = time.compare(that.time)
}

/**
 * Factory for [[Event]].
 */
object Event {
  /**
   * Create an event from a user identifier, a location and an instant.
   *
   * @param user     User identifier.
   * @param location Location.
   * @param time     Instant.
   */
  def apply(user: String, location: Location, time: ReadableInstant): Event = Event(user, location.toPoint, time.toInstant)
}