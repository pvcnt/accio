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

package fr.cnrs.liris.privamov.core.clustering

import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.privamov.core.model.{Event, Trace}

/**
 * A clusterer creates clusters of points from a sequence of events.
 */
trait Clusterer extends Serializable {
  /**
   * Perform the clustering of spatio-temporal points.
   *
   * @param events Temporally ordered list of events.
   * @return List of clusters.
   */
  def cluster(events: Seq[Event]): Seq[Cluster]

  /**
   * Perform the clustering of a mobility trace.
   *
   * @param trace Mobility trace.
   * @return List of clusters.
   */
  def cluster(trace: Trace): Seq[Cluster] = cluster(trace.events)
}

/**
 * A cluster is formed of a set of events.
 *
 * @param events Set of events.
 */
class Cluster(val events: Set[Event]) extends Serializable {
  /**
   * Compute the centroid of this cluster.
   */
  lazy val centroid = Point.centroid(events.map(_.point))

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .add("size", events.size)
      .toString
}

/**
 * A clusterer creating a cluster for each input event.
 */
object IdentityClusterer extends Clusterer {
  override def cluster(events: Seq[Event]): Seq[Cluster] = events.map(event => new Cluster(Set(event)))
}

/**
 * A clusterer creating no cluster.
 */
object NoClusterer extends Clusterer {
  override def cluster(events: Seq[Event]): Seq[Cluster] = Seq.empty
}