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
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.locapriv.model.Trace
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Duration

import scala.util.Random

/**
 * Unit tests for [[SpatialDistortionOp]].
 */
class SpatialDistortionOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  val eps = 1e-7

  behavior of "SpatialDistortion"

  it should "return zero for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(Seq(t1), Seq(t1), interpolate = false)
    metrics.avg(Me) shouldBe 0d
    metrics.min(Me) shouldBe 0d
    metrics.max(Me) shouldBe 0d
    metrics.median(Me) shouldBe 0d
    metrics.stddev(Me) shouldBe 0d
  }

  it should "return zero for temporally shifted traces" in {
    val t1 = randomTrace(Me, 120)
    val t2 = t1.replace { events =>
      var prev = events.head.time
      events.zipWithIndex.map { case (rec, idx) =>
        val now = prev.plus(Duration.standardSeconds(Random.nextInt(3600)))
        prev = now
        rec.copy(time = now)
      }
    }
    val metrics = execute(Seq(t1), Seq(t2), interpolate = false)
    metrics.avg(Me) shouldBe 0d
    metrics.min(Me) shouldBe 0d
    metrics.max(Me) shouldBe 0d
    metrics.median(Me) shouldBe 0d
    metrics.stddev(Me) shouldBe 0d
  }

  it should "compute spatial distortion" in {
    val t1 = randomFixedTrace(Me, 120)
    val distances = DenseVector(Seq.fill(120)(Random.nextInt(5000).toDouble): _*)
    val t2 = t1.replace { events =>
      events.zipWithIndex.map { case (rec, idx) =>
        rec.copy(point = rec.point.translate(S1Angle.degrees(Random.nextInt(360)), Distance.meters(distances(idx))))
      }
    }
    val metrics = execute(Seq(t1), Seq(t2), interpolate = true)
    metrics.avg(Me) shouldBe (mean(distances) +- eps)
    metrics.min(Me) shouldBe (min(distances) +- eps)
    metrics.max(Me) shouldBe (max(distances) +- eps)
    metrics.median(Me) shouldBe (median(distances) +- eps)
    metrics.stddev(Me) shouldBe (stddev(distances) +- eps)
  }

  private def execute(train: Seq[Trace], test: Seq[Trace], interpolate: Boolean) = {
    val trainDs = writeTraces(train: _*)
    val testDs = writeTraces(test: _*)
    new SpatialDistortionOp().execute(SpatialDistortionIn(train = trainDs, test = testDs, interpolate = interpolate), ctx)
  }
}