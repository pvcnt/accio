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

package fr.cnrs.liris.privamov.clustering

import com.google.common.base.Preconditions.checkArgument
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.privamov.model.Event

import scala.collection.mutable

/**
 * Density joinable clustering algorithm.
 *
 * Changqing Zhou, Dan Frankowski, Pamela Ludford, Shashi Shekhar and Loren
 * Terveen. Discovering Personal Gazetteers: An Interactive Clustering Approach.
 * In GIS 2004.
 */
class DJClusterer(epsilon: Distance, minPoints: Int) extends Clusterer {
  checkArgument(minPoints > 0)
  checkArgument(epsilon.meters > 0)

  override def cluster(events: Seq[Event]): Seq[Cluster] = {
    var clusters = mutable.ListBuffer.empty[Cluster]
    for (event <- events) {
      val neighborhood = neighbors(event.point, events)
      if (neighborhood.length >= minPoints) {
        val newCluster = mutable.HashSet.empty[Event]
        newCluster ++= neighborhood
        val intersecting = clusters.filter(cluster => cluster.events.intersect(newCluster).nonEmpty)
        intersecting.foreach(cluster => newCluster ++= cluster.events)
        clusters = clusters.diff(intersecting)
        clusters += new Cluster(newCluster.toSet)
      }
    }
    clusters
  }

  /**
   * Naive function returning neighbors of a given point.
   *
   * @param point A point we want to get the neighbors
   * @param world All available points
   * @return Points that are within a threshold fixed by { @link #epsilon}
   */
  private def neighbors(point: Point, world: Seq[Event]): Seq[Event] =
    world.filter(r => r.point.distance(point) <= epsilon)
}