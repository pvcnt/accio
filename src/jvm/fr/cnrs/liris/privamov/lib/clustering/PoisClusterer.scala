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
import fr.cnrs.liris.common.util.Distance
import fr.cnrs.liris.accio.core.model.{Poi, Record}
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
   * @param records
   * @return A set of POIs
   */
  def cluster(records: Iterable[Record]): Set[Poi] = {
    val stays = dtClusterer.cluster(records.toSeq)
    val staysAsRecords = stays.zipWithIndex.map {
      case (stay, idx) => Record(idx.toString, stay.centroid, Instant.now)
    }
    val pois = djClusterer.cluster(staysAsRecords)
    pois.map(poi => {
      Poi(poi.records.flatMap(record => stays(record.user.toInt).records))
    }).toSet
  }
}