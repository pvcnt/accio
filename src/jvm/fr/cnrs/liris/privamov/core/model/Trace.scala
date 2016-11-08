/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.geo.{BoundingBox, Distance, Point}

/**
 * A trace is a list of events belonging to a single user.
 *
 * Traces have an identifier allowing to track the lineage of transformations it went through. A trace identifier is
 * only intended to change only when a trace is split into multiple traces. It should begin with the user identifier,
 * and is followed by a unique identifier.
 *
 * @param id     Trace identifier.
 * @param user   User identifier.
 * @param events Chronologically ordered events.
 */
case class Trace private(id: String, user: String, events: Seq[Event]) {
  /**
   * Check if this trace is empty, i.e., contains no event.
   */
  def isEmpty: Boolean = events.isEmpty

  /**
   * Check if this trace is not empty, i.e., contains at least one event.
   */
  def nonEmpty: Boolean = events.nonEmpty

  /**
   * Return the size of the trace, i.e., the number of events it contains.
   */
  def size: Int = events.size

  /**
   * Return the total duration of this trace, i.e., the elapsed time between the first and
   * last event.
   */
  def duration: Duration = {
    if (events.isEmpty) {
      Duration.millis(0)
    } else {
      (events.head.time to events.last.time).duration
    }
  }

  /**
   * Return the total length of the trace, i.e., the distance that has been travelled.
   */
  def length: Distance = distances.foldLeft(Distance.Zero)(_ + _)

  /**
   * Return the sequence of distances between consecutive points. The number of items is the size
   * of this trace minus 1 (or 0 if the trace is empty).
   */
  def distances: Seq[Distance] =
  events.sliding(2).map(rs => rs.head.point.distance(rs.last.point)).toSeq

  /**
   * Return the sequence of durations between consecutive points. The number of durations is equal
   * to the size of this collection minus 1 (or 0 if the trace is already empty).
   */
  def durations: Seq[Duration] =
  events.sliding(2).map(rs => (rs.head.time to rs.last.time).duration).toSeq

  /**
   * Return the minimal bounding box encompassing all points of this trace.
   */
  def boundingBox: BoundingBox = {
    val minX = events.map(_.point.x).min
    val maxX = events.map(_.point.x).max
    val minY = events.map(_.point.y).min
    val maxY = events.map(_.point.y).max
    BoundingBox(Point(minX, minY), Point(maxX, maxY))
  }

  /**
   * Return a new trace after applying a given function on each event.
   *
   * @param fn A function modifying the events (it shouldn't modify the events' user)
   */
  def map(fn: Event => Event): Trace = new Trace(id, user, events.map(fn))

  /**
   * Return a new trace after applying a given function on each event.
   *
   * @param fn Whether to keep an event or not
   */
  def filter(fn: Event => Boolean): Trace = new Trace(id, user, events.filter(fn))

  /**
   * Return a new trace after applying a function on the sequence of events.
   *
   * @param fn A function modifying the events (it shouldn't modify the events' user)
   */
  def replace(fn: Seq[Event] => Seq[Event]): Trace = new Trace(id, user, fn(events))

  /**
   * Return a new trace with other events.
   *
   * @param events New events (should belong to the same user)
   */
  def replace(events: Seq[Event]): Trace = new Trace(id, user, events)

  /**
   * Return an empty trace with the same user.
   */
  def empty: Trace = new Trace(id, user, Seq.empty)

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("user", user)
      .toString
}

/**
 * Factory for [[Trace]].
 */
object Trace {
  /**
   * Create an empty trace.
   *
   * @param id Trace identifier.
   */
  def empty(id: String): Trace = new Trace(id, getUser(id), Seq.empty)

  /**
   * Create a trace from an non-empty and unordered list of events.
   *
   * @param events A non-empty list of events.
   */
  def apply(events: Iterable[Event]): Trace = {
    val seq = events.toSeq.sortBy(_.time)
    require(seq.nonEmpty, "Cannot create a trace from an empty list of events")
    new Trace(seq.head.user, seq.head.user, seq)
  }

  /**
   * Create a trace from an non-empty and already ordered list of events.
   *
   * @param events A non-empty list of events.
   */
  def apply(events: Seq[Event]): Trace = {
    require(events.nonEmpty, "Cannot create a trace from an empty list of events")
    new Trace(events.head.user, events.head.user, events)
  }

  /**
   * Create a trace from an non-empty and unordered list of events and a trace identifier. Events should be consistent
   * with given trace identifier.
   *
   * @param id     Trace identifier.
   * @param events A non-empty list of events.
   * @throws IllegalArgumentException If events are inconsistent with given trace identifier.
   */
  def apply(id: String, events: Iterable[Event]): Trace = {
    val user = getUser(id)
    require(events.isEmpty || events.head.user == user, s"Inconsistent trace identifier and event user: $id and ${events.head.user}")
    val seq = events.toSeq.sortBy(_.time)
    new Trace(id, user, seq)
  }

  /**
   * Create a trace from an non-empty and already ordered list of events and a trace identifier. Events should be
   * consistent with given trace identifier.
   *
   * @param id     Trace identifier.
   * @param events A non-empty list of events.
   * @throws IllegalArgumentException If events are inconsistent with given trace identifier.
   */
  def apply(id: String, events: Seq[Event]): Trace = {
    val user = getUser(id)
    require(events.isEmpty || events.head.user == user, s"Inconsistent trace identifier and event user: $id and ${events.head.user}")
    new Trace(id, user, events)
  }

  /**
   * Extract the user identifier from a trace identifier.
   *
   * @param id Trace identifier.
   */
  private def getUser(id: String) = id.split("-").head
}