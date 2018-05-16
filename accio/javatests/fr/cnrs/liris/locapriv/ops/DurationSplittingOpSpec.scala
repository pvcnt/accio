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

import scala.util.Random

/**
 * Unit tests for [[DurationSplittingOp]].
 */
class DurationSplittingOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "DurationSplittingOp"

  it should "split by duration" in {
    val trace = randomTrace(Me, 150, Duration.standardSeconds(Random.nextInt(10)))
    val res = transform(trace, Duration.standardSeconds(10))
    res.zipWithIndex.foreach { case (e, idx) =>
      e.id should startWith(s"$Me-")
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
    res.groupBy(_.id).values.foreach { vs =>
      (vs.last.time.millis - vs.head.time.millis) should be <= 10000L
    }
  }

  it should "handle a duration greater than trace's duration" in {
    val trace = randomTrace(Me, 60, Duration.standardSeconds(1))
    val res = transform(trace, Duration.standardSeconds(100))
    res.zipWithIndex.foreach { case (e, idx) =>
      e.id shouldBe s"$Me-0"
      e.time shouldBe trace(idx).time
      e.lat shouldBe trace(idx).lat
      e.lng shouldBe trace(idx).lng
    }
  }

  private def transform(data: Seq[Event], duration: Duration) = {
    com.twitter.jvm.numProcs.let(1) {
      val ds = writeTraces(data: _*)
      val res = DurationSplittingOp(duration = duration, data = ds).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}