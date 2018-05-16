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

import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataCompletenessOp]].
 */
class DataCompletenessOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "DataCompleteness"

  it should "compute the data completeness" in {
    val t1 = randomTrace(Me, 120)
    val t2 = randomTrace(Me, 81)
    val metrics = execute(t1, t2)
    metrics should contain theSameElementsAs Seq(DataCompletenessOp.Value(Me, 120, 81, 0.675))
  }

  it should "return 1 for identical traces" in {
    val t1 = randomTrace(Me, 120)
    val metrics = execute(t1, t1)
    metrics should contain theSameElementsAs Seq(DataCompletenessOp.Value(Me, 120, 120, 1))
  }

  it should "return 0 for an empty trace" in {
    val metrics = execute(randomTrace(Me, 85), randomTrace(Me, 0))
    metrics should contain theSameElementsAs Seq(DataCompletenessOp.Value(Me, 85, 0, 0))
  }

  private def execute(train: Seq[Event], test: Seq[Event]) = {
    com.twitter.jvm.numProcs.let(1) {
      val trainDs = writeTraces(train: _*)
      val testDs = writeTraces(test: _*)
      val res = DataCompletenessOp(trainDs, testDs).execute(ctx)
      env.read[DataCompletenessOp.Value].csv(res.metrics.uri).collect().toSeq
    }
  }
}