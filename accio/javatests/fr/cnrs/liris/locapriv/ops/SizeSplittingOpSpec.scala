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
 * Unit tests for [[SizeSplittingOp]].
 */
class SizeSplittingOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "SizeSplittingOp"

  it should "split by size" in {
    val trace = randomTrace(Me, 150)
    val res = transform(trace, 20)
    res.zipWithIndex.slice(0, 20).foreach { case (e, idx) =>
      e.id shouldBe "me-0"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(20, 40).foreach { case (e, idx) =>
      e.id shouldBe "me-1"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(40, 60).foreach { case (e, idx) =>
      e.id shouldBe "me-2"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(60, 80).foreach { case (e, idx) =>
      e.id shouldBe "me-3"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(80, 100).foreach { case (e, idx) =>
      e.id shouldBe "me-4"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(100, 120).foreach { case (e, idx) =>
      e.id shouldBe "me-5"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(120, 140).foreach { case (e, idx) =>
      e.id shouldBe "me-6"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.zipWithIndex.slice(140, 150).foreach { case (e, idx) =>
      e.id shouldBe "me-7"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
  }

  it should "handle a size greater than trace's size" in {
    val trace = randomTrace(Me, 60)
    val res = transform(trace, 65)
    res.zipWithIndex.foreach { case (e, idx) =>
      e.id shouldBe "me-0"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
  }

  it should "handle empty traces" in {
    val res = transform(Seq.empty, 2)
    res should have size 0
  }

  private def transform(data: Seq[Event], size: Int) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res = SizeSplittingOp(size = size, data = ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}