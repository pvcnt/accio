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

import fr.cnrs.liris.locapriv.model.Trace
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataCompletenessOp]].
 */
class DataCompletenessOpSpec extends UnitSpec with WithTraceGenerator with OperatorSpec {
  behavior of "DataCompleteness"

  it should "compute the data completeness" in {
    val t1 = randomTrace(Me, 120)
    val t2 = randomTrace(Me, 85)
    val metrics = execute(Seq(t1), Seq(t2))
    metrics.value shouldBe Map(Me -> (85d / 120))
  }

  it should "return one for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(Seq(t1), Seq(t1))
    metrics.value shouldBe Map(Me -> 1d)
  }

  it should "return nothing for an empty trace" in {
    val t1 = randomTrace(Me, 85)
    val t2 = randomTrace(Me, 0)
    val metrics = execute(Seq(t1), Seq(t2))
    metrics.value shouldBe Map(Me -> 0d)
  }

  private def execute(train: Seq[Trace], test: Seq[Trace]) = {
    val trainDs = writeTraces(train: _*)
    val testDs = writeTraces(test: _*)
    new DataCompletenessOp().execute(DataCompletenessIn(trainDs, testDs), ctx)
  }
}