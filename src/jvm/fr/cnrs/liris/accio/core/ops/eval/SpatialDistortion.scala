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

package fr.cnrs.liris.accio.core.ops.eval

import fr.cnrs.liris.accio.core.framework.{Evaluator, Metric, Op}
import fr.cnrs.liris.accio.core.model.Trace
import fr.cnrs.liris.accio.core.param.Param
import fr.cnrs.liris.common.geo.Point

@Op(
  category = "metric",
  help = "Compute spatial distortion between two datasets of traces"
)
case class SpatialDistortion(
    @Param(help = "Whether to interpolate between points")
    interpolate: Boolean = true
) extends Evaluator {
  override def evaluate(reference: Trace, result: Trace): Seq[Metric] = {
    require(reference.size > 1)
    val points = reference.records.map(_.point)
    val distances = if (interpolate) {
      evaluateWithInterpolation(points, result)
    } else {
      evaluateWithoutInterpolation(points, result)
    }
    MetricUtils.descriptiveStats(distances.map(_.meters))
  }

  override def metrics: Seq[String] = MetricUtils.descriptiveStatsMetrics

  private def evaluateWithoutInterpolation(reference: Seq[Point], result: Trace) =
    result.records.map(event => Point.nearest(event.point, reference).distance)

  private def evaluateWithInterpolation(reference: Seq[Point], result: Trace) = {
    result.records.map { record =>
      val (a, b) = nearestLine(record.point, reference)
      val projected = if (a == b) a else projectToLine(record.point, a, b)
      record.point.distance(projected)
    }
  }

  private def nearestLine(point: Point, line: Seq[Point]) = {
    val a = Point.nearest(point, line)
    val b = if (a.idx == 0) {
      line(1)
    } else if (a.idx == line.size - 1) {
      line(a.idx - 1)
    } else {
      Point.nearest(point, Seq(line(a.idx - 1), line(a.idx + 1))).point
    } //TODO: should check for previous/next non-identical point
    (a.point, b)
  }

  private def projectToLine(point: Point, a: Point, b: Point, snap: Boolean = true) = {
    // http://stackoverflow.com/questions/1459368/snap-point-to-a-line-java
    val apx = point.x - a.x
    val apy = point.y - a.y
    val abx = b.x - a.x
    val aby = b.y - a.y

    val ab2 = abx * abx + aby * aby
    val ap_ab = apx * abx + apy * aby
    var t = ap_ab / ab2
    if (snap) {
      if (t < 0) {
        t = 0
      } else if (t > 1) {
        t = 1
      }
    }
    a + Point(abx * t, aby * t)
  }
}
