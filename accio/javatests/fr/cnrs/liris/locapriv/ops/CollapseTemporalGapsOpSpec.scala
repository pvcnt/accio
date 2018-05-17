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

import fr.cnrs.liris.locapriv.domain.{Event, instantOrdering}
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{DateTime, Duration, Instant}

/**
 * Unit tests for [[CollapseTemporalGapsOp]].
 */
class CollapseTemporalGapsOpSpec extends UnitSpec with ScalaOperatorSpec with WithTraceGenerator {
  behavior of "CollapseTemporalGapsOp"

  it should "collapse temporal gaps" in {
    val trace = randomTrace(Me, 60, Duration.standardMinutes(1)) ++
      randomTrace(Me, 60, Duration.standardMinutes(1)).map { event =>
        event.copy(time = event.time.plus(Duration.standardDays(5)))
      } ++
      randomTrace(Me, 60, Duration.standardMinutes(1)).map { event =>
        event.copy(time = event.time.plus(Duration.standardDays(6)))
      }
    val startAt = DateTime.parse("2016-01-01T00:00:00Z").toInstant
    val res = execute(startAt, trace)
    res.foreach(_.id shouldBe Me)
    res.map(_.point) should contain theSameElementsInOrderAs trace.map(_.point)
    res.head.time should (be >= startAt and be <= startAt.plus(Duration.standardDays(1)))
    res.last.time should (be >= startAt.plus(Duration.standardDays(2)) and be <= startAt.plus(Duration.standardDays(3)))
  }

  private def execute(startAt: Instant, traces: Seq[Event]) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(traces: _*)
      val res = CollapseTemporalGapsOp(startAt, ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}
