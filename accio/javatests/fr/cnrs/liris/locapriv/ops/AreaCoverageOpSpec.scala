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

import com.google.common.geometry.{S2Cell, S2CellId, S2LatLng}
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.LatLng
import org.joda.time.{Duration, Instant}

import scala.util.Random

/**
 * Unit tests for [[AreaCoverageOp]].
 */
class AreaCoverageOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "AreaCoverageOp"

  private val eps = 1E-9

  it should "return 1 for identical traces" in {
    val t1 = randomTrace(Me, 120)
    var res = execute(t1, t1, 14, None)
    res should contain theSameElementsAs Seq(MetricUtils.FscoreValue(Me, 1, 1, 1))

    res = execute(t1, t1, 14, Some(Duration.standardMinutes(30)))
    res should contain theSameElementsAs Seq(MetricUtils.FscoreValue(Me, 1, 1, 1))
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
      val t1 = Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(2).toPoint, now))
      val t2 = Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(4).toPoint, now))
      val res = execute(t1, t2, level, None)
      res should have size 1
      res.head.id shouldBe Me
      res.head.precision shouldBe 1d / 2
      res.head.recall shouldBe closeTo(2d / 3, eps)
      res.head.fscore shouldBe closeTo(2d * 1 / 2 * 2 / 3 * 1 / (1d / 2 + 2d / 3), eps)
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
      val t1 = Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(1).toPoint, now),
        Event(Me, pts(2).toPoint, now))
      val t2 = Seq(
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now),
        Event(Me, pts(0).toPoint, now.plus(width.getMillis + 1)),
        Event(Me, pts(1).toPoint, now.plus(width.getMillis + 1)),
        Event(Me, pts(1).toPoint, now.minus(width.getMillis + 1)),
        Event(Me, pts(2).toPoint, now),
        Event(Me, pts(3).toPoint, now.minus(width.getMillis + 1)),
        Event(Me, pts(3).toPoint, now),
        Event(Me, pts(4).toPoint, now))
      val res = execute(t1, t2, level, Some(width))
      res should have size 1
      res.head.id shouldBe Me
      res.head.precision shouldBe 2d / 8
      res.head.recall shouldBe closeTo(2d / 3, eps)
      res.head.fscore shouldBe closeTo(2d * 2 / 8 * 2 / 3 * 1 / (2d / 8 + 2d / 3), eps)
    }
  }

  private def execute(train: Seq[Event], test: Seq[Event], level: Int, bucketSize: Option[Duration]) = {
    com.twitter.jvm.numProcs.let(1) {
      val trainDs = writeTraces(train: _*)
      val testDs = writeTraces(test: _*)
      val res = AreaCoverageOp(train = trainDs, test = testDs, level = level, width = bucketSize).execute(ctx)
      env.read[MetricUtils.FscoreValue].csv(res.metrics.uri).collect().toSeq
    }
  }
}