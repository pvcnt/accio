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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.Instant

/**
 * This clusterer finds stays that are repeated, namely points of interests. This is actually a
 * merge between DJClusterer and DTClusterer, where the DJ-clustering is used to cluster the output
 * of the DT-clusterer.
 *
 * Vincent Primault, Sonia Ben Mokhtar, Cédric Lauradoux and Lionel Brunie. Differentially Private
 * Location Privacy in Practice. In MOST'14.
 */
class RepeatedPoisClusterer(minDuration: Duration, maxDiameter: Distance, minPoints: Int)
  extends PoisClusterer {

  private[this] val dtClusterer = new DTClusterer(minDuration, maxDiameter)
  private[this] val djClusterer = new DJClusterer(maxDiameter / 2, minPoints)

  /**
   * Perform clustering of spatio-temporal points.
   *
   * @param events Temporally ordered list of events.
   * @return List of POIs.
   */
  override def clusterPois(events: Seq[Event]): Seq[Poi] = {
    val stays = dtClusterer.cluster(events)
    val staysAsEvents = stays.zipWithIndex.map {
      case (stay, idx) => Event(idx.toString, stay.centroid, Instant.now)
    }
    val pois = djClusterer.cluster(staysAsEvents)
    pois.map { poi =>
      Poi(poi.events.flatMap(event => stays(event.user.toInt).events))
    }
  }

  /**
   * Perform clustering of spatio-temporal points.
   *
   * @param events Temporally ordered list of events.
   * @return List of Clusters.
   */
  override def cluster(events: Seq[Event]): Seq[Cluster] = {
    val stays = dtClusterer.cluster(events)
    val staysAsEvents = stays.zipWithIndex.map {
      case (stay, idx) => Event(idx.toString, stay.centroid, Instant.now)
    }
    val pois = djClusterer.cluster(staysAsEvents)
    pois.map { poi =>
      Cluster(poi.events.flatMap(event => stays(event.user.toInt).events))
    }
  }
}
