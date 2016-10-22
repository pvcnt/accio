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

package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.stats.AggregatedStats
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv
import org.joda.time.Instant

@Op(
  category = "metric",
  help = "Compute temporal distortion difference between two datasets of traces")
class TemporalDistortionOp @Inject()(env: SparkleEnv) extends Operator[TemporalDistortionIn, TemporalDistortionOut] with SparkleOperator {

  override def execute(in: TemporalDistortionIn, ctx: OpContext): TemporalDistortionOut = {
    val train = read(in.train, env)
    val test = read(in.test, env)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res) }.toArray
    TemporalDistortionOut(
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

  private def interpolate(trace: Trace, time: Instant) =
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

case class TemporalDistortionIn(
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class TemporalDistortionOut(
  @Arg(help = "Temporal distortion min") min: Map[String, Double],
  @Arg(help = "Temporal distortion max") max: Map[String, Double],
  @Arg(help = "Temporal distortion stddev") stddev: Map[String, Double],
  @Arg(help = "Temporal distortion avg") avg: Map[String, Double],
  @Arg(help = "Temporal distortion median") median: Map[String, Double])