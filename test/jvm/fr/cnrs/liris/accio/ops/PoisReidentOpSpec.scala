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

package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.accio.ops.model.{Event, Poi, PoiSet}
import fr.cnrs.liris.accio.ops.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[PoisReidentOp]].
 */
class PoisReidentOpSpec extends UnitSpec with OperatorSpec with WithTraceGenerator {
  behavior of "PoisReidentOp"

  private[this] lazy val trainDs = {
    val refPois1 = PoiSet("user1", Set(
      Poi(Set(Event("user1", Point(5, 5), Now))),
      Poi(Set(Event("user1", Point(0, 0), Now)))))
    val refPois2 = PoiSet("user2", Set(
      Poi(Set(Event("user2", Point(10, 8), Now))),
      Poi(Set(Event("user2", Point(1, 1), Now)))))
    val refPois3 = PoiSet("user3", Set(
      Poi(Set(Event("user3", Point(8, 6), Now))),
      Poi(Set(Event("user3", Point(-2, -9), Now)))))
    writePois(refPois1, refPois2, refPois3)
  }

  it should "identify all users when ran on the same data" in {
    val res = new PoisReidentOp().execute(ReidentificationIn(trainDs, trainDs), ctx)
    res.rate shouldBe 1
    res.matches("user1") shouldBe "user1"
    res.matches("user2") shouldBe "user2"
    res.matches("user3") shouldBe "user3"
    //res.distances("user1").find(_._1 == "user1").get._2 shouldBe 0
    //res.distances("user2").find(_._1 == "user2").get._2 shouldBe 0
    //res.distances("user3").find(_._1 == "user3").get._2 shouldBe 0
  }

  it should "identify users" in {
    val resPois1 = PoiSet("user1", Set(
      Poi(Set(Event("user1", Point(4, 4), Now)))
    ))
    val resPois2 = PoiSet("user2", Set(
      Poi(Set(Event("user2", Point(9, 7), Now))),
      Poi(Set(Event("user2", Point(-1, -10), Now)))
    ))
    val resPois3 = PoiSet("user3", Set(
      Poi(Set(Event("user3", Point(9, 7), Now))),
      Poi(Set(Event("user3", Point(2, 2), Now)))
    ))
    val testDs = writePois(resPois1, resPois2, resPois3)

    val res = new PoisReidentOp().execute(ReidentificationIn(trainDs, testDs), ctx)
    res.rate shouldBe closeTo(1d / 3, 0.001)
    res.matches("user1") shouldBe "user1"
    res.matches("user2") shouldBe "user3"
    res.matches("user3") shouldBe "user2"
  }
}