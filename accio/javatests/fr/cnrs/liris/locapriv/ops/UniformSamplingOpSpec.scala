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
import fr.cnrs.liris.locapriv.domain.Trace
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
    val res1 = transform(Seq(trace), 0.1)
    res1.flatMap(_.events).forall(trace.events.contains) shouldBe true
    // Why isn't it equivalent?!?
    //res1.flatMap(_.events) should contain only (trace.events: _*)
    res1.map(_.size).sum.toDouble shouldBe (10d +- 3)

    val res2 = transform(Seq(trace), 0.5)
    res2.flatMap(_.events).forall(trace.events.contains) shouldBe true
    res2.map(_.size).sum.toDouble shouldBe (50d +- 3)

    val res3 = transform(Seq(trace), 0.9)
    res3.flatMap(_.events).forall(trace.events.contains) shouldBe true
    res3.map(_.size).sum.toDouble shouldBe (90d +- 3)
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 0)
    res should have size 1
    res.head.id shouldBe trace.id
    res.head.events should have size 0
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 1)
    res should have size 1
    res.head.id shouldBe trace.id
    res.head.events should contain theSameElementsInOrderAs trace.events
  }

  private def transform(data: Seq[Trace], probability: Double) = {
    val ds = writeTraces(data: _*)
    val res = UniformSamplingOp(probability = probability, data = ds).execute(ctx)
    readTraces(res.data)
  }
}
