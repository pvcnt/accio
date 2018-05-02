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

import fr.cnrs.liris.locapriv.domain.Trace
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EnforceSizeOp]].
 */
class EnforceSizeOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "EnforceSizeOp"

  it should "keep traces with a size greater than min threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 10) should contain theSameElementsInOrderAs Seq(trace)
    transformMinSize(Seq(trace), 15) should contain theSameElementsInOrderAs Seq(trace)
  }

  it should "reject traces with a size lower than min threshold" in {
    val trace = randomTrace(Me, 15)
    transformMinSize(Seq(trace), 16) should have size 0
    transformMinSize(Seq(trace), 20) should have size 0
  }

  it should "shorten traces with a size greater than max threshold" in {
    val trace = randomTrace(Me, size = 15)
    transformMaxSize(Seq(trace), 10) should contain theSameElementsInOrderAs Seq(trace.replace(_.take(10)))
  }

  it should "keep traces with a size lower than max threshold" in {
    val trace = randomTrace(Me, size = 15)
    transformMaxSize(Seq(trace), 15) should contain theSameElementsInOrderAs Seq(trace)
    transformMaxSize(Seq(trace), 20) should contain theSameElementsInOrderAs Seq(trace)
  }

  private def transformMinSize(data: Seq[Trace], size: Int) = {
    val ds = writeTraces(data: _*)
    val res = EnforceSizeOp(minSize = Some(size), maxSize = None, data = ds).execute(ctx)
    readTraces(res.data)
  }

  private def transformMaxSize(data: Seq[Trace], size: Int) = {
    val ds = writeTraces(data: _*)
    val res = EnforceSizeOp(minSize = None, maxSize = Some(size), data = ds).execute(ctx)
    readTraces(res.data)
  }
}