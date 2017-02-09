/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.ops

import com.google.common.geometry.S2CellId
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.privamov.core.model.Trace
import org.joda.time.{Duration, Instant}

@Op(
  category = "metric",
  help = "Compute area coverage difference between two datasets of traces",
  cpu = 4,
  ram = "2G")
class AreaCoverageOp extends Operator[AreaCoverageIn, AreaCoverageOut] {
  override def execute(in: AreaCoverageIn, ctx: OpContext): AreaCoverageOut = {
    val train = ctx.read[Trace](in.train)
    val test = ctx.read[Trace](in.test)
    val metrics = train.zip(test).map { case (ref, res) => evaluate(ref, res, in.level, in.width) }.toArray
    AreaCoverageOut(
      precision = metrics.map { case (k, v) => k -> v._1 }.toMap,
      recall = metrics.map { case (k, v) => k -> v._2 }.toMap,
      fscore = metrics.map { case (k, v) => k -> v._3 }.toMap)
  }

  private def evaluate(ref: Trace, res: Trace, level: Int, bucketSize: Option[Duration]) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val refCells = getCells(ref, level, bucketSize)
    val resCells = getCells(res, level, bucketSize)
    val matched = resCells.intersect(refCells).size
    val metrics = (
      MetricUtils.precision(resCells.size, matched),
      MetricUtils.recall(refCells.size, matched),
      MetricUtils.fscore(refCells.size, resCells.size, matched))
    (ref.id, metrics)
  }

  private def getCells(trace: Trace, level: Int, bucketSize: Option[Duration]) = {
    trace.events.map { rec =>
      bucketSize match {
        case None => truncate(rec.point.toLatLng, level).toString
        case Some(precision) => truncate(rec.point.toLatLng, level) + "|" + truncate(rec.time, precision)
      }
    }.toSet
  }

  private def truncate(latLng: LatLng, level: Int): Long = S2CellId.fromLatLng(latLng.toS2).parent(level).id

  private def truncate(instant: Instant, precision: Duration): Long = instant.getMillis / precision.getMillis
}

case class AreaCoverageIn(
  @Arg(help = "S2 cells levels") level: Int,
  @Arg(help = "Width of time buckets") width: Option[Duration],
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class AreaCoverageOut(
  @Arg(help = "Area coverage precision") precision: Map[String, Double],
  @Arg(help = "Area coverage recall") recall: Map[String, Double],
  @Arg(help = "Area coverage F-score") fscore: Map[String, Double])