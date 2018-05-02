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

import com.google.common.geometry.S2CellId
import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.locapriv.domain.Trace
import fr.cnrs.liris.util.geo.LatLng
import org.joda.time.{Duration, Instant}

@Op(
  category = "metric",
  help = "Compute area coverage difference between two datasets of traces",
  cpus = 4,
  ram = "2G")
case class AreaCoverageOp(
  @Arg(help = "S2 cells levels") level: Int,
  @Arg(help = "Width of time buckets") width: Option[Duration],
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)
  extends ScalaOperator[AreaCoverageOut] with SparkleOperator {

  override def execute(ctx: OpContext): AreaCoverageOut = {
    val trainDs = read[Trace](train)
    val testDs = read[Trace](test)
    val metrics = trainDs.zip(testDs).map { case (ref, res) => evaluate(ref, res) }.toArray
    AreaCoverageOut(
      precision = metrics.map { case (k, v) => k -> v._1 }.toMap,
      recall = metrics.map { case (k, v) => k -> v._2 }.toMap,
      fscore = metrics.map { case (k, v) => k -> v._3 }.toMap)
  }

  private def evaluate(ref: Trace, res: Trace) = {
    require(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val refCells = getCells(ref, level)
    val resCells = getCells(res, level)
    val matched = resCells.intersect(refCells).size
    val metrics = (
      MetricUtils.precision(resCells.size, matched),
      MetricUtils.recall(refCells.size, matched),
      MetricUtils.fscore(refCells.size, resCells.size, matched))
    (ref.id, metrics)
  }

  private def getCells(trace: Trace, level: Int) = {
    trace.events.map { rec =>
      width match {
        case None => truncate(rec.point.toLatLng, level).toString
        case Some(w) => truncate(rec.point.toLatLng, level) + "|" + truncate(rec.time, w)
      }
    }.toSet
  }

  private def truncate(latLng: LatLng, level: Int): Long = S2CellId.fromLatLng(latLng.toS2).parent(level).id

  private def truncate(instant: Instant, precision: Duration): Long = instant.getMillis / precision.getMillis
}

case class AreaCoverageOut(
  @Arg(help = "Area coverage precision") precision: Map[String, Double],
  @Arg(help = "Area coverage recall") recall: Map[String, Double],
  @Arg(help = "Area coverage F-score") fscore: Map[String, Double])