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
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.geo.Point

/**
 * Applies gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.
 */
@Op(
  help = "Applies gaussian kernel smoothing on traces"
)
case class GaussianKernelSmoothingOp(
    @Param(help = "Bandwidth")
    omega: Duration
) extends Mapper {
  override def map(trace: Trace): Trace =
    trace.transform(_.map { record =>
      var ks = 0d
      var x = 0d
      var y = 0d
      for (i <- trace.records.indices) {
        val k = gaussianKernel(record.time.millis, trace.records(i).time.millis)
        ks += k
        x += k * trace.records(i).point.x
        y += k * trace.records(i).point.y
      }
      x /= ks
      y /= ks
      record.copy(point = Point(x, y))
    })

  private def gaussianKernel(t1: Long, t2: Long): Double =
    Math.exp(-Math.pow(t1 - t2, 2) / (2 * omega.millis * omega.millis))
}