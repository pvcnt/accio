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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.privamov.core.model.Event

import scala.collection.mutable

/**
 * Density-time clustering algorithm performing the extraction of stays. A stay
 * is defined by a minimal amount of time spent in the same place (defined by a
 * maximum distance between two points of the cluster).
 *
 * R. Hariharan and K. Toyama. Project Lachesis: parsing and modeling
 * location histories. Geographic Information Science, 2004.
 *
 * @param minDuration Minimum amount of time spent inside a stay.
 * @param maxDiameter Maximum diameter of a stay.
 */
class DTClusterer(minDuration: Duration, maxDiameter: Distance) extends Clusterer {
  override def cluster(events: Seq[Event]): Seq[Cluster] = {
    val clusters = mutable.ListBuffer.empty[Cluster]
    val candidate = mutable.ListBuffer.empty[Event]
    events.foreach(doCluster(_, candidate, clusters))
    handleCandidate(candidate, clusters)
    clusters
  }

  /**
   * Recursive clustering routine.
   *
   * @param event     Current event.
   * @param candidate Current list of tuples being a candidate stay.
   * @param clusters  Current list of clusters.
   */
  private def doCluster(event: Event, candidate: mutable.ListBuffer[Event], clusters: mutable.ListBuffer[Cluster]): Unit = {
    if (candidate.isEmpty || isInDiameter(event, candidate)) {
      candidate += event
    } else if (handleCandidate(candidate, clusters)) {
      candidate += event
    } else {
      candidate.remove(0)
      doCluster(event, candidate, clusters)
    }
  }

  /**
   * Check if a event can be added to a candidate cluster without breaking the distance requirement.
   *
   * @param event     Event to test.
   * @param candidate Collection of events.
   * @return True if the tuple can be safely added, false other.
   */
  private def isInDiameter(event: Event, candidate: Seq[Event]) =
  candidate.forall(_.point.distance(event.point) <= maxDiameter)

  /**
   * Check if a cluster is valid w.r.t. the time threshold.
   *
   * @param candidate Current candidate cluster.
   * @param clusters  Current list of clusters.
   * @return True if the stay is valid, false otherwise.
   */
  private def handleCandidate(candidate: mutable.ListBuffer[Event], clusters: mutable.ListBuffer[Cluster]) = {
    if (candidate.size <= 1) {
      false
    } else if ((candidate.head.time to candidate.last.time).duration < minDuration) {
      false
    } else {
      clusters += new Cluster(candidate.toSet)
      candidate.clear()
      true
    }
  }
}