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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[UniformSamplingOp]].
 */
class UniformSamplingOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  behavior of "UniformSamplingOp"

  it should "downsample traces" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    // Seed is fixed, no need for multiple runs.
    transform(Seq(trace), 0.1).map(_.size).sum.toDouble shouldBe (10d +- 3)
    transform(Seq(trace), 0.5).map(_.size).sum.toDouble shouldBe (50d +- 3)
    transform(Seq(trace), 0.9).map(_.size).sum.toDouble shouldBe (90d +- 3)
  }

  it should "handle null probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 0)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should have size 0
  }

  it should "handle certain probability" in {
    val trace = randomTrace(Me, 100, 10.seconds)
    val res = transform(Seq(trace), 1)
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events shouldBe trace.events
  }

  private def transform(data: Seq[Trace], probability: Double) = {
    val ds = writeTraces(data: _*)
    val res = new UniformSamplingOp().execute(UniformSamplingIn(probability = probability, data = ds), ctx)
    readTraces(res.data)
  }
}
