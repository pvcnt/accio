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
 * Unit tests for [[EnforceDurationOp]].
 */
class EnforceDurationOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "EnforceDurationOp"

  it should "keep traces with a duration greater than min threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(trace, Duration.standardMinutes(10)) should contain theSameElementsAs trace
    transformMinDuration(trace, Duration.standardMinutes(14)) should contain theSameElementsAs trace
  }

  it should "reject traces with a duration lower than min threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    transformMinDuration(trace, Duration.standardMinutes(15)) should have size 0
    transformMinDuration(trace, Duration.standardMinutes(20)) should have size 0
  }

  it should "shorten traces with a duration greater than max threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    val data = transformMaxDuration(trace, Duration.standardSeconds(10 * 60 + 10))
    data should contain theSameElementsInOrderAs trace.take(11)
  }

  it should "keep traces with a duration lower than max threshold" in {
    val trace = randomTrace(Me, size = 15, rate = Duration.standardMinutes(1))
    var data = transformMaxDuration(trace, Duration.standardMinutes(14))
    data should contain theSameElementsInOrderAs trace.take(15)

    data = transformMaxDuration(trace, Duration.standardMinutes(20))
    data should contain theSameElementsInOrderAs trace.take(15)
  }

  private def transformMaxDuration(data: Seq[Event], duration: Duration) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res = EnforceDurationOp(maxDuration = Some(duration), minDuration = None, data = ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }

  private def transformMinDuration(data: Seq[Event], duration: Duration) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res = EnforceDurationOp(minDuration = Some(duration), maxDuration = None, data = ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}