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

import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{DateTime, Duration, Instant}
import fr.cnrs.liris.dal.core.api.Values.instantOrdering

/**
 * Unit tests for [[CollapseTemporalGapsOp]].
 */
class CollapseTemporalGapsOpSpec extends UnitSpec with OperatorSpec with WithTraceGenerator {
  behavior of "CollapseTemporalGapsOp"

  it should "collapse temporal gaps" in {
    val trace = randomTrace(Me, 60, Duration.standardMinutes(1)) ++
      randomTrace(Me, 60, Duration.standardMinutes(1)).replace { events =>
        events.map(event => event.copy(time = event.time.plus(Duration.standardDays(5))))
      } ++
      randomTrace(Me, 60, Duration.standardMinutes(1)).replace { events =>
        events.map(event => event.copy(time = event.time.plus(Duration.standardDays(6))))
      }
    val startAt = DateTime.parse("2016-01-01T00:00:00Z").toInstant
    val res = execute(startAt, Seq(trace))
    res should have size 1
    res.head.user shouldBe Me
    res.head.events.map(_.point) should contain theSameElementsInOrderAs trace.events.map(_.point)
    res.head.events.head.time should (be >= startAt and be <= startAt.plus(Duration.standardDays(1)))
    res.head.events.last.time should (be >= startAt.plus(Duration.standardDays(2)) and be <= startAt.plus(Duration.standardDays(3)))
  }

  private def execute(startAt: Instant, traces: Seq[Trace]) = {
    val ds = writeTraces(traces: _*)
    val res = new CollapseTemporalGapsOp().execute(CollapseTemporalGapsIn(startAt, ds), ctx)
    readTraces(res.data)
  }
}
