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
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[UniformSamplingOp]].
 */
class UniformSamplingOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "UniformSamplingOp"

  it should "downsample traces" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    // Seed is fixed, no need for multiple runs.
    var res = transform(trace, 0.1)
    res.foreach(trace.contains(_) shouldBe true)
    res.size.toDouble shouldBe (10d +- 3)

    res = transform(trace, 0.5)
    res.foreach(trace.contains(_) shouldBe true)
    res.size.toDouble shouldBe (50d +- 3)

    res = transform(trace, 0.9)
    res.foreach(trace.contains(_) shouldBe true)
    res.size.toDouble shouldBe (90d +- 3)
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(trace, 0)
    res should have size 0
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(trace, 1)
    res should contain theSameElementsInOrderAs trace
  }

  it should "handle empty traces" in {
    val res = transform(Seq.empty, 0.5)
    res should have size 0
  }

  private def transform(data: Seq[Event], probability: Double) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res = UniformSamplingOp(probability = probability, data = ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}
