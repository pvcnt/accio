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

import breeze.linalg.{DenseVector, max, min}
import breeze.stats._
import com.google.common.geometry.S1Angle
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.Duration

import scala.util.Random

/**
 * Unit tests for [[SpatialDistortionOp]].
 */
class SpatialDistortionOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "SpatialDistortion"

  private val eps = 1e-7

  it should "compute spatial distortion" in {
    val t1 = randomFixedTrace(Me, 120)
    val distances = DenseVector(Seq.fill(120)(Random.nextInt(5000).toDouble): _*)
    val t2 = {
      t1.zipWithIndex.map { case (e, idx) =>
        e.withPoint(e.point.translate(S1Angle.degrees(Random.nextInt(360)), Distance.meters(distances(idx))))
      }
    }
    val metrics = execute(t1, t2, interpolate = false)
    metrics should have size 1
    metrics.head.n shouldBe 120
    metrics.head.avg shouldBe (mean(distances) +- eps)
    metrics.head.min shouldBe (min(distances) +- eps)
    metrics.head.max shouldBe (max(distances) +- eps)
    metrics.head.p50 shouldBe (median(distances) +- eps)
    metrics.head.stddev shouldBe (stddev(distances) +- eps)
  }

  it should "handle identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(t1, t1, interpolate = false)
    metrics should contain theSameElementsAs Seq(MetricUtils.StatsValue(Me, 120, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
  }

  it should "handle temporally shifted traces" in {
    val t1 = randomTrace(Me, 120)
    val t2 = {
      var prev = t1.head.time
      t1.map { e =>
        val now = prev.plus(Duration.standardSeconds(Random.nextInt(3600)))
        prev = now
        e.copy(time = now)
      }
    }
    val metrics = execute(t1, t2, interpolate = false)
    metrics should contain theSameElementsAs Seq(MetricUtils.StatsValue(Me, 120, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
  }

  private def execute(train: Seq[Event], test: Seq[Event], interpolate: Boolean) = {
    com.twitter.jvm.numProcs.let(1) {
      val trainDs = writeTraces(train: _*)
      val testDs = writeTraces(test: _*)
      val res = SpatialDistortionOp(train = trainDs, test = testDs, interpolate = interpolate).execute(ctx)
      env.read[MetricUtils.StatsValue].csv(res.metrics.uri).collect().toSeq
    }
  }
}