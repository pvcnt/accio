/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.privamov.core.model

import java.util.Objects

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.common.geo.Distance
import org.joda.time.Instant

/**
 * A Point of Interest is a place where a user has spent some time. We only keep summarized information here (instead
 * of keeping the whole set of events forming that POI) to save some memory and allow easy (de)serialization.
 *
 * There is no concept of "empty POI", if a POI exists it means that it is useful.
 *
 * @param user     User identnfier.
 * @param centroid Centroid of this POI.
 * @param size     Number of events forming this POI.
 * @param start    First time the user has been inside inside this POI.
 * @param end      Last time the user has been inside inside this POI.
 * @param diameter Diameter of this POI (i.e., the distance between the two farthest points).
 */
case class Poi(
  user: String,
  centroid: Point,
  size: Int,
  start: Instant,
  end: Instant,
  diameter: Distance) {

  /**
   * Return the total amount of time spent inside this POI.
   */
  def duration: Duration = (start to end).duration

  /**
   * We consider two POIs are the same if they belong to the same user and are defined by the same centroid during
   * the same time window (we do not consider the other attributes).
   *
   * @param that Another object.
   * @return True if they represent the same POI, false otherwise.
   */
  override def equals(that: Any): Boolean = that match {
    case p: Poi => p.user == user && p.centroid == centroid && p.start == start && p.end == end
    case _ => false
  }

  override def hashCode: Int = Objects.hash(user, centroid, start, end)
}

object Poi {
  /**
   * Create a new POI from a list of events.
   *
   * @param events A non-empty list of events
   */
  def apply(events: Iterable[Event]): Poi = {
    val seq = events.toSeq.sortBy(_.time)
    require(seq.nonEmpty, "Cannot create a POI from an empty list of events")
    val centroid = Point.centroid(seq.map(_.point))
    val diameter = Point.fastDiameter(seq.map(_.point))
    new Poi(seq.head.user, centroid, seq.size, seq.head.time, seq.last.time, diameter)
  }

  /**
   * Create a POI from a single point and timestamp. Its duration will be zero and its diameter arbitrarily fixed
   * to 10 meters.
   *
   * @param user  User identifier
   * @param point Location of this POI
   * @param time  Timestamp
   */
  def apply(user: String, point: Point, time: Instant): Poi =
  Poi(user, point, 1, time, time, Distance.meters(10))

  /**
   * Create a POI from a single event. Its duration will be zero and its diameter arbitrarily fixed to 10 meters.
   *
   * @param event Event
   */
  def apply(event: Event): Poi = apply(event.user, event.point, event.time)
}