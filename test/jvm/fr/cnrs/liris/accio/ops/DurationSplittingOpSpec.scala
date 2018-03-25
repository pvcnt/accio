/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.ops.model.Trace
import fr.cnrs.liris.accio.ops.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

import scala.util.Random

/**
 * Unit tests for [[DurationSplittingOp]].
 */
class DurationSplittingOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  behavior of "DurationSplittingOp"

  it should "split by duration" in {
    val trace = randomTrace(Me, 150, Duration.standardSeconds(Random.nextInt(10)))
    val res = transform(Seq(trace), Duration.standardSeconds(10))
    res.foreach { trace =>
      trace.user shouldBe Me
      trace.duration.seconds should be <= 10L
    }
    res.flatMap(_.events) should contain theSameElementsInOrderAs trace.events
  }

  it should "handle a duration greater than trace's duration" in {
    val trace = randomTrace(Me, 60, Duration.standardSeconds(1))
    val res = transform(Seq(trace), Duration.standardSeconds(100))
    res should have size 1
    res.head.user shouldBe trace.user
    res.head.events should contain theSameElementsInOrderAs trace.events
  }

  private def transform(data: Seq[Trace], duration: Duration) = {
    val ds = writeTraces(data: _*)
    val res = new DurationSplittingOp().execute(DurationSplittingIn(duration = duration, data = ds), ctx)
    readTraces(res.data)
  }
}