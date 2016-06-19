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

package fr.cnrs.liris.privamov.service.gateway.store

import fr.cnrs.liris.common.geo.BoundingBox
import fr.cnrs.liris.accio.core.model.Record
import org.joda.time.DateTime

case class View(source: Option[String] = None, startAfter: Option[DateTime] = None, endBefore: Option[DateTime] = None, area: Option[BoundingBox] = None) {
  def accessible(id: String): Boolean = source.contains(id) || source.isEmpty

  def filter(records: Seq[Record]): Seq[Record] = {
    if (source.isDefined || startAfter.isDefined || endBefore.isDefined || area.isDefined) {
      records.filter(filter)
    } else {
      records
    }
  }

  def restrict(restrictions: Iterable[View]): Set[View] = {
    if (restrictions.exists(_.includes(this))) {
      Set(this)
    } else {
      restrictions.flatMap(_.restrict(this)).toSet
    }
  }

  def restrict(other: View): Option[View] = {
    val newSource = reconciliate(source, other.source) { (a, b) =>
      if (a == b) Some(Some(a)) else None
    } match {
      case Some(s) => s
      case None => return None
    }
    val newStartAfter = reconciliate(startAfter, other.startAfter) { (a, b) =>
      Some(Some(max(a, b)))
    } match {
      case Some(d) => d
      case None => return None
    }
    val newEndBefore = reconciliate(endBefore, other.endBefore) { (a, b) =>
      Some(Some(min(a, b)))
    } match {
      case Some(d) => d
      case None => return None
    }
    val newArea = reconciliate(area, other.area) { (a, b) =>
      if (a.intersects(b)) Some(Some(a.intersection(b))) else None
    } match {
      case Some(a) => a
      case None => return None
    }
    Some(View(newSource, newStartAfter, newEndBefore, newArea))
  }

  def includes(other: View): Boolean = {
    (source.isEmpty || other.source.contains(source.get)) &&
        (startAfter.isEmpty || other.startAfter.forall(o => o == startAfter.get || o.isAfter(startAfter.get))) &&
        (endBefore.isEmpty || other.endBefore.forall(o => o == endBefore.get || o.isBefore(endBefore.get))) &&
        (area.isEmpty || other.area.forall(_.contained(area.get)))
  }

  private def max(a: DateTime, b: DateTime) = if (a.isAfter(b)) a else b

  private def min(a: DateTime, b: DateTime) = if (a.isBefore(b)) a else b

  private def reconciliate[T](a: Option[T], b: Option[T])(fn: (T, T) => Option[Option[T]]): Option[Option[T]] = {
    if (a.isEmpty) {
      Some(b)
    } else if (b.isEmpty) {
      Some(a)
    } else {
      fn(a.get, b.get)
    }
  }

  private def filter(record: Record) = {
    startAfter.forall(record.time.isAfter) && endBefore.forall(record.time.isBefore) &&
        area.forall(_.contains(record.point)) && source.forall(_.contains(record.user))
  }
}

object View {
  def everything: View = new View()
}

case class Source(
    id: String,
    firstSeen: Option[DateTime] = None,
    lastSeen: Option[DateTime] = None,
    count: Option[Long] = None)

/**
 * An event store is responsible for retrieving collected data.
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 */
trait EventStore {
  /**
   * Return this event store name.
   */
  def name: String

  /**
   * Check whether this data provider contains data for the given source.
   *
   * @param id A source identifier
   * @return True if this source exists, false otherwise
   */
  def contains(id: String): Boolean

  /**
   * Return the sources this event store is composed of, ordered in lexicographical order of source
   * identifier.
   *
   * @param limit      Maximum number of sources to retrieve (ignored if <= 0)
   * @param startAfter Source identifier to start after
   */
  def sources(views: Set[View], limit: Option[Int] = None, startAfter: Option[String] = None): Seq[Source]

  def countSources(views: Set[View]): Int

  /**
   * Return metadata about a source.
   *
   * @param id A source identifier
   * @throws IllegalArgumentException If the source does not exist
   */
  @throws[IllegalArgumentException]
  def apply(id: String): Source

  /**
   * Return features from this event store.
   */
  def features(views: Set[View], limit: Option[Int] = None, sample: Boolean = false): Seq[Record]

  def countFeatures(views: Set[View]): Int
}

/**
 * Factory for event stores.
 *
 * @author Vincent Primault <vincent.primault@liris.cnrs.fr>
 */
trait EventStoreFactory {
  /**
   * Create a new event store for the given name (unique among all event stores).
   *
   * @param name A name for the event store
   */
  def create(name: String): EventStore
}