/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.locapriv.model.Trace

@Op(
  category = "metric",
  help = "Compute spatial distortion between two datasets of traces",
  cpu = 3,
  ram = "6G")
class SpatialDistortionOp extends Operator[SpatialDistortionIn, SpatialDistortionOut] with SparkleOperator {
  override def execute(in: SpatialDistortionIn, ctx: OpContext): SpatialDistortionOut = {
    val train = read[Trace](in.train)
    val test = read[Trace](in.test)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res, in.interpolate) }.toArray
    SpatialDistortionOut(
      min = metrics.map { case (k, v) => k -> v.min }.toMap,
      max = metrics.map { case (k, v) => k -> v.max }.toMap,
      stddev = metrics.map { case (k, v) => k -> v.stddev }.toMap,
      avg = metrics.map { case (k, v) => k -> v.avg }.toMap,
      median = metrics.map { case (k, v) => k -> v.median }.toMap)
  }

  private def evaluate(ref: Trace, res: Trace, interpolate: Boolean) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    require(res.isEmpty || ref.size >= 1, s"Cannot evaluate spatial distortion with empty reference trace ${ref.id}")
    val points = ref.events.map(_.point)
    val distances = if (interpolate) {
      evaluateWithInterpolation(points, res)
    } else {
      evaluateWithoutInterpolation(points, res)
    }
    ref.id -> AggregatedStats(distances.map(_.meters))
  }

  private def evaluateWithoutInterpolation(reference: Seq[Point], result: Trace) =
    result.events.map(event => Point.nearest(event.point, reference).distance)

  private def evaluateWithInterpolation(reference: Seq[Point], result: Trace) = {
    result.events.map { event =>
      if (reference.size == 1) {
        event.point.distance(reference.head)
      } else {
        val (a, b) = nearestLine(event.point, reference)
        val projected = if (a == b) a else projectToLine(event.point, a, b)
        event.point.distance(projected)
      }
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

case class SpatialDistortionIn(
  @Arg(help = "Whether to interpolate between points") interpolate: Boolean = true,
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class SpatialDistortionOut(
  @Arg(help = "Spatial distortion min") min: Map[String, Double],
  @Arg(help = "Spatial distortion max") max: Map[String, Double],
  @Arg(help = "Spatial distortion stddev") stddev: Map[String, Double],
  @Arg(help = "Spatial distortion avg") avg: Map[String, Double],
  @Arg(help = "Spatial distortion median") median: Map[String, Double])