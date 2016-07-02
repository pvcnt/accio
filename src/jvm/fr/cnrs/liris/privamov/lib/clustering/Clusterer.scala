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

import com.google.common.base.MoreObjects
import fr.cnrs.liris.accio.core.model.{Event, Trace}
import fr.cnrs.liris.common.geo.Point

/**
 * A clusterer creates clusters of points from a sequence of events.
 */
trait Clusterer extends Serializable {
  /**
   * Perform clustering of spatio-temporal points.
   *
   * @param events A temporally ordered list of events
   * @return A list of cluters
   */
  def cluster(events: Seq[Event]): Seq[Cluster]

  def cluster(trace: Trace): Seq[Cluster] = cluster(trace.events)
}

/**
 * A cluster is formed of a set of events.
 *
 * @param events A set of events
 */
class Cluster(val events: Set[Event]) extends Serializable {
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