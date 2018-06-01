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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import org.joda.time.Instant

@Op(
  category = "metric",
  help = "Compute temporal distortion difference between two datasets of traces.")
case class SpatioTemporalDistortionOp(
  @Arg(help = "Train dataset")
  train: RemoteFile,
  @Arg(help = "Test dataset")
  test: RemoteFile)
  extends ScalaOperator[SpatioTemporalDistortionOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): SpatioTemporalDistortionOp.Out = {
    val trainDs = read[Event](train).groupBy(_.id)
    val testDs = read[Event](test).groupBy(_.id)
    val metrics = trainDs.join(testDs)(evaluate)
    SpatioTemporalDistortionOp.Out(write(metrics, 0, ctx))
  }

  private def evaluate(id: String, ref: Iterable[Event], res: Iterable[Event]): Iterable[MetricUtils.StatsValue] = {
    val (larger, smaller) = if (ref.size > res.size) (ref, res) else (res, ref)
    val distances = smaller.map { event =>
      event.point.distance(interpolate(larger, event.time)).meters
    }
    Iterable(MetricUtils.stats(id, distances))
  }

  private def interpolate(trace: Iterable[Event], time: Instant) = {
    if (time.isBefore(trace.head.time)) {
      trace.head.point
    } else if (time.isAfter(trace.last.time)) {
      trace.last.point
    } else {
      val between = trace.sliding(2).find { recs =>
        time.compareTo(recs.head.time) >= 0 && time.compareTo(recs.last.time) <= 0
      }.get
      if (time == between.head.time) {
        between.head.point
      } else if (time == between.last.time) {
        between.last.point
      } else {
        val ratio = (between.head.time to time).millis.toDouble / (between.head.time to between.last.time).millis
        between.head.point.interpolate(between.last.point, ratio)
      }
    }
  }
}

object SpatioTemporalDistortionOp {

  case class Out(
    @Arg(help = "Metrics dataset")
    metrics: RemoteFile)

}