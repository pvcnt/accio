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

package fr.cnrs.liris.locapriv.domain

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

class TraceSpec extends UnitSpec with WithTraceGenerator {
  behavior of "Trace"

  it should "return its duration" in {
    val t1 = randomTrace(Me, 101, Duration.standardSeconds(10))
    t1.duration shouldBe Duration.standardSeconds(1000)

    val t2 = Trace.empty(Me)
    t2.duration shouldBe Duration.millis(0)
  }

  it should "return its size" in {
    val t1 = randomTrace(Me, 101, Duration.standardSeconds(10))
    t1.size shouldBe 101

    val t2 = Trace.empty(Me)
    t2.size shouldBe 0
  }

  it should "return its user" in {
    val t1 = Trace(Me, Seq(Event(Me, Here, Now)))
    t1.user shouldBe Me

    val t2 = Trace.empty(Me)
    t2.user shouldBe Me
  }
}