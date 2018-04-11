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
import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.util.Requirements._
import fr.cnrs.liris.locapriv.model.Trace
import org.joda.time.Instant

@Op(
  category = "metric",
  help = "Compute temporal distortion difference between two datasets of traces",
  cpu = 3,
  ram = "6G")
class SpatioTemporalDistortionOp extends Operator[SpatioTemporalDistortionIn, SpatioTemporalDistortionOut] with SparkleOperator {
  override def execute(in: SpatioTemporalDistortionIn, ctx: OpContext): SpatioTemporalDistortionOut = {
    val train = read[Trace](in.train)
    val test = read[Trace](in.test)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res) }.toArray
    SpatioTemporalDistortionOut(
      min = metrics.map { case (k, v) => k -> v.min }.toMap,
      max = metrics.map { case (k, v) => k -> v.max }.toMap,
      stddev = metrics.map { case (k, v) => k -> v.stddev }.toMap,
      avg = metrics.map { case (k, v) => k -> v.avg }.toMap,
      median = metrics.map { case (k, v) => k -> v.median }.toMap)
  }

  private def evaluate(ref: Trace, res: Trace) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val (larger, smaller) = if (ref.size > res.size) (ref, res) else (res, ref)
    val distances = smaller.events.map { rec =>
      rec.point.distance(interpolate(larger, rec.time)).meters
    }
    ref.id -> AggregatedStats(distances)
  }

  private def interpolate(trace: Trace, time: Instant) = {
    if (time.isBefore(trace.events.head.time)) {
      trace.events.head.point
    } else if (time.isAfter(trace.events.last.time)) {
      trace.events.last.point
    } else {
      val between = trace.events.sliding(2).find { recs =>
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

case class SpatioTemporalDistortionIn(
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class SpatioTemporalDistortionOut(
  @Arg(help = "Temporal distortion min") min: Map[String, Double],
  @Arg(help = "Temporal distortion max") max: Map[String, Double],
  @Arg(help = "Temporal distortion stddev") stddev: Map[String, Double],
  @Arg(help = "Temporal distortion avg") avg: Map[String, Double],
  @Arg(help = "Temporal distortion median") median: Map[String, Double])