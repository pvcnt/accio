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
 * Unit tests for [[EnforceDurationOp]].
 */
class EnforceDurationOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  behavior of "EnforceDurationOp"

  it should "keep traces with a duration greater than min threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(Seq(trace), Duration.standardMinutes(10)) should contain theSameElementsAs Seq(trace)
    transformMinDuration(Seq(trace), Duration.standardMinutes(14)) should contain theSameElementsAs Seq(trace)
  }

  it should "reject traces with a duration lower than min threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(Seq(trace), Duration.standardMinutes(15)) should have size 0
    transformMinDuration(Seq(trace), Duration.standardMinutes(20)) should have size 0
  }

  it should "shorten traces with a duration greater than max threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val data = transformMaxDuration(Seq(trace), Duration.standardSeconds(10 * 60 + 10))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 11)
  }

  it should "keep traces with a duration lower than max threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    var data = transformMaxDuration(Seq(trace), Duration.standardMinutes(14))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 15)
    data = transformMaxDuration(Seq(trace), Duration.standardMinutes(20))
    data should have size 1
    assertTraceIsShortened(trace, data.head, 15)
  }

  private def assertTraceIsShortened(t: Trace, t1: Trace, s1: Int) = {
    t1.user shouldBe t.user
    t1.events shouldBe t.events.take(s1)
  }

  private def transformMaxDuration(data: Seq[Trace], duration: Duration) = {
    val ds = writeTraces(data: _*)
    val res = new EnforceDurationOp().execute(EnforceDurationIn(maxDuration = Some(duration), minDuration = None, data = ds), ctx)
    readTraces(res.data)
  }

  private def transformMinDuration(data: Seq[Trace], duration: Duration) = {
    val ds = writeTraces(data: _*)
    val res = new EnforceDurationOp().execute(EnforceDurationIn(minDuration = Some(duration), maxDuration = None, data = ds), ctx)
    readTraces(res.data)
  }
}