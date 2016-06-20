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

package fr.cnrs.liris.accio.core.ops.transform

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.core.framework.{Mapper, Op}
import fr.cnrs.liris.accio.core.model.{Record, Trace}
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.util.Distance

import scala.collection.mutable

/**
 * An implementation of the speed smoothing algorithm.
 *
 * Vincent Primault, Sonia Ben Mokhtar, Cédric Lauradoux, Lionel Brunie. Time Distortion
 * Anonymization for the Publication of Mobility Data with High Utility. In Proceedings of
 * TrustCom'15.
 */
@Op(
  category = "lppm",
  help = "Enforce speed smoothing guarantees on traces"
)
case class PromesseOp(
    @Param(help = "Distance to enforce between two consecutive points")
    epsilon: Distance
) extends Mapper {

  override def map(trace: Trace): Trace =
    if (trace.isEmpty) {
      trace.empty
    } else if (epsilon == Distance.Zero) {
      trace
    } else {
      // We sample records to keep those at a distance of exactly `epsilon` from the previous one.
      // Sampled locations will be interpolated linearly between the two nearest reported
      // locations. This way there will be the same distance between two consecutive records.
      val sampled = sample(trace.records)

      // The time to "spend" will be uniformely allocated. This way there will be the same
      // duration between two consecutive records.
      trace.copy(records = allocate(sampled))
    }

  private def sample(records: Seq[Record]) = {
    var sampled = mutable.ListBuffer.empty[Record]
    var prev: Option[Record] = None
    for (record <- records) {
      if (prev.isDefined) {
        var d = record.point.distance(prev.get.point)
        while (d >= epsilon) {
          // Generate as many points as needed to get from previous to current location by steps
          // of epsilon.
          val ratio = epsilon.meters / d.meters
          val newPoint = prev.get.point.interpolate(record.point, ratio)
          sampled += record.copy(point = newPoint)

          prev = Some(record.copy(point = newPoint))
          d = record.point.distance(prev.get.point)
        }
      } else {
        //First iteration, keep true location and time.
        sampled += record
        prev = Some(record)
      }
    }
    // We skip the potential first and last stay to maximize utility (there will likely be a stay
    // that will "consume" time budget) and sightly protect start and end points.
    if (sampled.nonEmpty) {
      sampled = sampled.drop(1)
    }
    if (sampled.nonEmpty) {
      sampled = sampled.dropRight(1)
    }
    sampled
  }

  private def allocate(sampled: Seq[Record]) =
    if (sampled.size <= 2) {
      sampled
    } else {
      val from = sampled.head.time
      val timeSpent = (from to sampled.last.time).millis
      val interval = timeSpent.toDouble / (sampled.size - 1)
      sampled.zipWithIndex.map { case (record, i) =>
        val shift = math.ceil(i * interval).toInt
        record.copy(time = from + shift)
      }
    }
}