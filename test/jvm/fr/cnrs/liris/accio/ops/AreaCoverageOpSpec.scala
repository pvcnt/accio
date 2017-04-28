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

package fr.cnrs.liris.accio.ops

import com.google.common.geometry.{S2Cell, S2CellId, S2LatLng}
import fr.cnrs.liris.common.geo.LatLng
import fr.cnrs.liris.accio.ops.model.{Event, Trace}
import fr.cnrs.liris.accio.ops.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{Duration, Instant}

import scala.util.Random

/**
 * Unit tests for [[AreaCoverageOp]].
 */
class AreaCoverageOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  private[this] val eps = 1E-9

  behavior of "AreaCoverageOp"

  it should "return 1 for identical traces" in {
    val t1 = randomTrace(Me, 120)
    var res = execute(Seq(t1), Seq(t1), 14, None)
    res.precision(Me) shouldBe 1d
    res.recall(Me) shouldBe 1d
    res.fscore(Me) shouldBe 1d

    res = execute(Seq(t1), Seq(t1), 14, Some(Duration.standardMinutes(30)))
    res.precision(Me) shouldBe 1d
    res.recall(Me) shouldBe 1d
    res.fscore(Me) shouldBe 1d
  }

  it should "compute area coverage w.r.t. level" in {
    val now = Instant.now()
    for (level <- 0 until 30) {
      val pt = randomLocation()
      val cell = S2CellId.fromLatLng(pt.toS2).parent(level)
      val pts = Seq(
        pt,
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap().nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap().nextWrap().nextWrap()).getCenter)))
      val t1 = Trace(Me, Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(2).toPoint, now)))
      val t2 = Trace(Me, Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(4).toPoint, now)))
      val res = execute(Seq(t1), Seq(t2), level, None)
      res.precision(Me) shouldBe 1d / 2
      res.recall(Me) shouldBe closeTo(2d / 3, eps)
      res.fscore(Me) shouldBe closeTo(2d * 1 / 2 * 2 / 3 * 1 / (1d / 2 + 2d / 3), eps)
    }
  }

  it should "compute area coverage w.r.t. bucket size" in {
    val now = Instant.now()
    val level = 10
    for (width <- Seq(Duration.standardSeconds(30), Duration.standardMinutes(5), Duration.standardMinutes(30), Duration.standardHours(1), Duration.standardDays(1))) {
      val pt = LatLng.degrees(Random.nextDouble() * 180 - 90, Random.nextDouble() * 360 - 180)
      val cell = S2CellId.fromLatLng(pt.toS2).parent(level)
      val pts = Seq(
        pt,
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap().nextWrap()).getCenter)),
        LatLng(new S2LatLng(new S2Cell(cell.nextWrap().nextWrap().nextWrap().nextWrap()).getCenter)))

      val t1 = Trace(Me, Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(2).toPoint, now)))
      val t2 = Trace(Me, Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now.plus(width.getMillis + 1)),
        Event(Me, pts(1).toPoint, now.plus(width.getMillis + 1)),
        Event(Me, pts(1).toPoint, now.minus(width.getMillis + 1)),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(3).toPoint, now.minus(width.getMillis + 1)),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(4).toPoint, now)))
      val res = execute(Seq(t1), Seq(t2), level, Some(width))
      res.precision(Me) shouldBe 2d / 8
      res.recall(Me) shouldBe closeTo(2d / 3, eps)
      res.fscore(Me) shouldBe closeTo(2d * 2 / 8 * 2 / 3 * 1 / (2d / 8 + 2d / 3), eps)
    }
  }

  private def execute(train: Seq[Trace], test: Seq[Trace], level: Int, bucketSize: Option[Duration]) = {
    val trainDs = writeTraces(train: _*)
    val testDs = writeTraces(test: _*)
    new AreaCoverageOp().execute(AreaCoverageIn(train = trainDs, test = testDs, level = level, width = bucketSize), ctx)
  }
}