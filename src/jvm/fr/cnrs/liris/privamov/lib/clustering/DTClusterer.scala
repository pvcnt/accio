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

package fr.cnrs.liris.privamov.lib.clustering

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.model.Event
import fr.cnrs.liris.common.util.Distance

import scala.collection.mutable

/**
 * Density-time clustering algorithm performing the extraction of stays. A stay
 * is defined by a minimal amount of time spent in the same place (defined by a
 * maximum distance between two points of the cluster).
 *
 * R. Hariharan and K. Toyama. Project Lachesis: parsing and modeling
 * location histories. Geographic Information Science, 2004.
 *
 * @param minDuration Minimum amount of time spent inside a stay
 * @param maxDiameter Maximum diameter of a stay (in meters)
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
   * @param event     Current event
   * @param candidate Current list of tuples being a candidate stay
   * @param clusters  Current list of clusters
   */
  private def doCluster(event: Event, candidate: mutable.ListBuffer[Event], clusters: mutable.ListBuffer[Cluster]): Unit =
    if (candidate.isEmpty || isInDiameter(event, candidate)) {
      candidate += event
    } else if (handleCandidate(candidate, clusters)) {
      candidate += event
    } else {
      candidate.remove(0)
      doCluster(event, candidate, clusters)
    }

  /**
   * Check if a event can be added to a candidate cluster without breaking the
   * distance requirement.
   *
   * @param event     Event to test
   * @param candidate Collection of events
   * @return True if the tuple can be safely added, false other
   */
  private def isInDiameter(event: Event, candidate: Seq[Event]) =
    candidate.forall(_.point.distance(event.point) <= maxDiameter)

  /**
   * Check if a cluster is valid w.r.t. the time threshold.
   *
   * @param candidate Current candidate cluster
   * @param clusters  Current list of clusters
   * @return True if the stay is valid, false otherwise
   */
  private def handleCandidate(candidate: mutable.ListBuffer[Event], clusters: mutable.ListBuffer[Cluster]) =
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