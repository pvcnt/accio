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
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.util.geo.LatLng
import org.joda.time.{Duration, Instant}

import scala.collection.mutable

@Op(
  category = "metric",
  help = "Compute area coverage difference between two datasets of traces.")
case class AreaCoverageOp(
  @Arg(help = "S2 cells levels")
  level: Int,
  @Arg(help = "Width of time buckets")
  width: Option[Duration],
  @Arg(help = "Train dataset")
  train: RemoteFile,
  @Arg(help = "Test dataset")
  test: RemoteFile)
  extends ScalaOperator[AreaCoverageOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): AreaCoverageOp.Out = {
    val trainDs = read[Event](train).groupBy(_.id)
    val testDs = read[Event](test).groupBy(_.id)
    val metrics = trainDs.join(testDs)(evaluate)
    AreaCoverageOp.Out(write(metrics, 0, ctx))
  }

  private def evaluate(id: String, ref: Iterable[Event], res: Iterable[Event]) = {
    val refCells = getCells(ref, level)
    val resCells = getCells(res, level)
    val matched = resCells.intersect(refCells).size
    Seq(MetricUtils.fscore(id, refCells.size, resCells.size, matched))
  }

  private def getCells(trace: Iterable[Event], level: Int) = {
    val cells = mutable.Set.empty[String]
    // Doing a foreach (instead of a map/toSet) seems to be faster, as we only insert de-duplicated
    // cell identifiers.
    trace.foreach { rec =>
      width match {
        case None => cells += truncate(rec.point.toLatLng, level).toString
        case Some(w) => cells += truncate(rec.point.toLatLng, level) + "|" + truncate(rec.time, w)
      }
    }
    cells
  }

  private def truncate(latLng: LatLng, level: Int): Long = S2CellId.fromLatLng(latLng.toS2).parent(level).id

  private def truncate(instant: Instant, precision: Duration): Long = instant.getMillis / precision.getMillis
}

object AreaCoverageOp {

  case class Out(
    @Arg(help = "Area coverage metrics")
    metrics: RemoteFile)

}