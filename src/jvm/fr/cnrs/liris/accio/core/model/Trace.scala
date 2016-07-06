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

import com.github.nscala_time.time.Imports._
import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.util.{Distance, Speed}

/**
 * A trace is a list of events belonging to a single user. Traces have an identifier allowing to
 * track the lineage of transformations it went through. A trace identifier is only intended to
 * change only when a trace is splitted into multiple traces.
 *
 * @param id     Trace identifier
 * @param user   User identifier
 * @param events Temporally ordered events
 */
class Trace(val id: String, val user: String, val events: Seq[Event]) {
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
  def duration: Duration =
    if (events.isEmpty) {
      Duration.millis(0)
    } else {
      (events.head.time to events.last.time).duration
    }

  /**
   * Return the total length of the trace, i.e., the distance that has been travelled.
   */
  def length: Distance = distances.foldLeft(Distance.Zero)(_ + _)

  /**
   * Return the sequence of instantaneous speeds between consecutive points. The number of items
   * is the size of this trace minus 1 (or 0 if the trace is empty).
   */
  def speeds: Seq[Speed] =
    events.sliding(2).map(rs => {
      val dl = rs.head.point.distance(rs.last.point)
      val dt = (rs.head.time to rs.last.time).duration
      Speed(dl, dt)
    }).toSeq

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
   * Return a list of traces after applying a function on the sequence of events.
   *
   * @param fn
   * @return
   */
  def split(fn: Seq[Event] => Seq[Trace]): Seq[Trace] =
    fn(events).zipWithIndex.map { case (trace, idx) =>
      new Trace(s"$id/$idx", trace.user, trace.events)
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

  /**
   * Return a trace with the same batch but with all events associated to another user name.
   *
   * @param user Another user name
   */
  def pseudonymise(user: String): Trace = new Trace(id, user, events.map(_.copy(user = user)))

  override def toString: String = MoreObjects.toStringHelper(this).addValue(id).toString
}

/** Factory for [[Trace]]. */
object Trace {
  /**
   * Create an empty trace associated with a given user.
   *
   * @param user A user name
   */
  def empty(user: String): Trace = new Trace(user, user, Seq.empty)

  /**
   * Create a trace from a list of events. The user name will be automatically inferred from this data, and the
   * events will be temporally ordered.
   *
   * @param events A non-empty list of events
   */
  def apply(events: Iterable[Event]): Trace = {
    val seq = events.toSeq.sortBy(_.time)
    require(seq.nonEmpty, "Cannot create a trace from an empty list of events")
    new Trace(seq.head.user, seq.head.user, seq)
  }

  def apply(user: String, events: Iterable[Event]): Trace = {
    val seq = events.toSeq.sortBy(_.time)
    new Trace(user, user, seq)
  }
}