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

package fr.cnrs.liris.locapriv.domain

import java.util.Objects

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.util.geo.{Distance, Point}
import org.joda.time.Instant

/**
 * A Point of Interest is a place where a user has spent some time. We only keep summarized
 * information here (instead of keeping the whole set of events forming that POI).
 *
 * There is no concept of "empty POI", if a POI exists it means that it is useful.
 *
 * @param id        Trace identifier.
 * @param centroid  Centroid of this POI.
 * @param size      Number of events forming this POI.
 * @param firstSeen First time the user has been inside inside this POI.
 * @param lastSeen  Last time the user has been inside inside this POI.
 * @param diameter  Diameter of this POI (i.e., the distance between the two farthest points).
 */
case class Poi(
  id: String,
  centroid: Point,
  size: Int,
  firstSeen: Instant,
  lastSeen: Instant,
  diameter: Distance) {

  /**
   * Return the user identifier associated with this trace.
   */
  def user: String = id.split("-").head

  /**
   * Return the total amount of time spent inside this POI.
   */
  def duration: Duration = (firstSeen to lastSeen).duration

  /**
   * We consider two POIs are the same if they belong to the same user (and not trace), and are
   * defined by the same centroid during the same time window (we do not consider the other
   * attributes).
   *
   * @param that Another object.
   * @return True if they represent the same POI, false otherwise.
   */
  override def equals(that: Any): Boolean = that match {
    case p: Poi =>
      p.user == user && p.centroid == centroid && p.firstSeen == firstSeen && p.lastSeen == lastSeen
    case _ => false
  }

  override def hashCode: Int = Objects.hash(user, centroid, firstSeen, lastSeen)
}

object Poi {
  /**
   * Create a new POI from a list of events.
   *
   * @param events A non-empty list of events.
   */
  def apply(events: Iterable[Event]): Poi = {
    val sorted = events.toSeq.sortBy(_.time)
    require(sorted.nonEmpty, "Cannot create a POI from an empty list of events")
    val centroid = Point.centroid(sorted.map(_.point))
    val diameter = Point.fastDiameter(sorted.map(_.point))
    new Poi(sorted.head.id, centroid, sorted.size, sorted.head.time, sorted.last.time, diameter)
  }

  /**
   * Create a POI from a single point and timestamp. Its duration will be zero and its diameter
   * arbitrarily fixed to 10 meters.
   *
   * @param id    Trace identifier.
   * @param point Location of this POI.
   * @param time  Timestamp.
   */
  def apply(id: String, point: Point, time: Instant): Poi =
    Poi(id, point, 1, time, time, Distance.meters(10))

  /**
   * Create a POI from a single event. Its duration will be zero and its diameter arbitrarily
   * fixed to 10 meters.
   *
   * @param event Event.
   */
  def apply(event: Event): Poi = apply(event.id, event.point, event.time)
}