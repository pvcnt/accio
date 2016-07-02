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

package fr.cnrs.liris.accio.core.model

import java.sql.Timestamp

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.common.geo.Point
import org.joda.time.Instant

/**
 * The smallest piece of information of our model. It is a discrete event associated with a user,
 * that occured at an instant and a specific place. It can embed additional numeric properties.
 *
 * @param user  A user identifier
 * @param point A geographical point
 * @param time  A temporal moment
 * @param props Additional numeric properties
 */
case class Event(user: String, point: Point, time: Instant, props: Map[String, Double]) extends Ordered[Event] {
  def set(key: String, value: Double): Event = copy(props = props + (key -> value))

  override def compare(that: Event): Int = time.compare(that.time)
}

/**
 * Factory for [[Event]].
 */
object Event {
  /**
   * Create an event with no additional properties.
   *
   * @param user  A user identifier
   * @param point A geographical point
   * @param time  A temporal moment
   */
  def apply(user: String, point: Point, time: Instant): Event =
    new Event(user, point, time, Map.empty)

  def apply(user: String, point: Point, time: Timestamp): Event =
    new Event(user, point, new Instant(time.getTime), Map.empty)

  def apply(user: String, point: Point, time: Timestamp, props: Map[String, Double]): Event =
    new Event(user, point, new Instant(time.getTime), props)
}