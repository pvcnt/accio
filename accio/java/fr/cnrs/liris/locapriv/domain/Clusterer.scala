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

import com.google.common.base.MoreObjects
import fr.cnrs.liris.util.geo.{Distance, Point}
import org.joda.time.Duration

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
}

trait PoisClusterer extends Clusterer {
  def clusterPois(events: Seq[Event]): Seq[Poi]
}

/**
 * A cluster is formed of a set of events.
 *
 * @param events List of temporally-ordered events.
 */
case class Cluster(events: Seq[Event]) {
  require(events.nonEmpty, "Cannot create a cluster of empty events")

  /**
   * Return the centroid of this cluster.
   */
  lazy val centroid: Point = Point.centroid(events.map(_.point))

  /**
   * Return the total duration spent inside his cluster.
   */
  lazy val duration = new Duration(events.head.time, events.last.time)

  /**
   * Return the diameter of this cluster.
   */
  lazy val diameter: Distance = Point.exactDiameter(events.map(_.point))

  override def toString: String = MoreObjects.toStringHelper(this).add("size", events.size).toString
}