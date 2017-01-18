/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import fr.cnrs.liris.privamov.core.model.{Event, Poi}
import org.joda.time.Instant

/**
 * This clusterer finds stays that are repeated, namely points of interests. This is actually a merge between
 * DJClusterer and DTClusterer, where the DJ-clustering is used to cluster the output of the DT-clusterer.
 *
 * Vincent Primault, Sonia Ben Mokhtar, Cédric Lauradoux and Lionel Brunie. Differentially Private
 * Location Privacy in Practice. In MOST'14.
 */
class PoisClusterer(minDuration: Duration, maxDiameter: Distance, minPoints: Int) extends Serializable {
  private[this] val dtClusterer = new DTClusterer(minDuration, maxDiameter)
  private[this] val djClusterer = new DJClusterer(maxDiameter / 2, minPoints)

  /**
   * Perform clustering of spatio-temporal points.
   *
   * @param events Temporally ordered list of events.
   * @return List of POIs.
   */
  def cluster(events: Iterable[Event]): Set[Poi] = {
    val stays = dtClusterer.cluster(events.toSeq)
    val staysAsEvents = stays.zipWithIndex.map {
      case (stay, idx) => Event(idx.toString, stay.centroid, Instant.now)
    }
    val pois = djClusterer.cluster(staysAsEvents)
    pois.map { poi =>
      Poi(poi.events.flatMap(event => stays(event.user.toInt).events))
    }.toSet
  }
}