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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.util.geo.{Distance, Point}

@Op(
  category = "metric",
  help = "Compute spatial distortion between two datasets of traces")
case class SpatialDistortionOp(
  @Arg(help = "Train dataset")
  train: RemoteFile,
  @Arg(help = "Test dataset")
  test: RemoteFile,
  @Arg(help = "Whether to interpolate between points")
  interpolate: Boolean = true)
  extends ScalaOperator[SpatialDistortionOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): SpatialDistortionOp.Out = {
    val trainDs = read[Event](train).groupBy(_.id)
    val testDs = read[Event](test).groupBy(_.id)
    val metrics = trainDs.join(testDs)(evaluate)
    SpatialDistortionOp.Out(write(metrics, 0, ctx))
  }

  private def evaluate(id: String, ref: Iterable[Event], res: Iterable[Event]): Iterable[MetricUtils.StatsValue] = {
    require(ref.nonEmpty, s"Cannot evaluate spatial distortion with empty reference trace")
    val points = ref.map(_.point)
    val distances = if (interpolate) {
      evaluateWithInterpolation(points, res)
    } else {
      evaluateWithoutInterpolation(points, res)
    }
    Iterable(MetricUtils.stats(id, distances.map(_.meters)))
  }

  private def evaluateWithoutInterpolation(reference: Iterable[Point], result: Iterable[Event]): Iterable[Distance] = {
    result.map(event => Point.nearest(event.point, reference).distance)
  }

  private def evaluateWithInterpolation(reference: Iterable[Point], result: Iterable[Event]): Iterable[Distance] = {
    result.map { event =>
      if (reference.size == 1) {
        event.point.distance(reference.head)
      } else {
        val (a, b) = nearestLine(event.point, reference.toSeq)
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

object SpatialDistortionOp {

  case class Out(
    @Arg(help = "Metrics dataset")
    metrics: RemoteFile)

}