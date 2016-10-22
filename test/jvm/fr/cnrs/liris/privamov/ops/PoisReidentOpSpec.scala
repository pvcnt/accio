/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import org.joda.time.Instant

import fr.cnrs.liris.common.geo.{LatLng, Point}
import fr.cnrs.liris.privamov.core.model.{Event, Poi, PoiSet}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[PoisReidentOp]].
 */
/*class PoisReidentOpSpec extends UnitSpec with SparkleOperator {
  val Here = LatLng.degrees(48.858222, 2.2945).toPoint
  val Now = Instant.now
  val Me = "me"
  val refPois = {
    val refPois1 = PoiSet("user1", Set(
      Poi(Set(Event("user1", Point(5, 5), Now))),
      Poi(Set(Event("user1", Point(0, 0), Now)))))
    val refPois2 = PoiSet("user2", Set(
      Poi(Set(Event("user2", Point(10, 8), Now))),
      Poi(Set(Event("user2", Point(1, 1), Now)))))
    val refPois3 = PoiSet("user3", Set(
      Poi(Set(Event("user3", Point(8, 6), Now))),
      Poi(Set(Event("user3", Point(-2, -9), Now)))))
    new FakeDataset(Map(
      "user1" -> Set(refPois1),
      "user2" -> Set(refPois2),
      "user3" -> Set(refPois3)))
  }

  "PoisReident" should "identify all users when ran on the same data" in {
    val reid = (new PoisReident).identify(refPois, refPois)
    reid.successRate shouldBe 1
    reid.trainingUsers should contain theSameElementsAs Set(UserId("user1"), UserId("user2"), UserId("user3"))
    reid(UserId("user1")).matches.find(_._1 == UserId("user1")).get._2.get shouldBe 0
    reid(UserId("user2")).matches.find(_._1 == UserId("user2")).get._2.get shouldBe 0
    reid(UserId("user3")).matches.find(_._1 == UserId("user3")).get._2.get shouldBe 0
  }

  it should "identify users" in {
    val resPois1 = PoisSet(UserId("user1"), Set(
      Poi(Set(Record(UserId("user1"), Point(4, 4), Now)))
    ))
    val resPois2 = PoisSet(UserId("user2"), Set(
      Poi(Set(Record(UserId("user2"), Point(9, 7), Now))),
      Poi(Set(Record(UserId("user2"), Point(-1, -10), Now)))
    ))
    val resPois3 = PoisSet(UserId("user3"), Set(
      Poi(Set(Record(UserId("user3"), Point(9, 7), Now))),
      Poi(Set(Record(UserId("user3"), Point(2, 2), Now)))
    ))
    val res = new FakeDataset(Map(
      UserId("user1") -> Set(resPois1),
      UserId("user2") -> Set(resPois2),
      UserId("user3") -> Set(resPois3)
    ))

    val reid = (new PoisReident).identify(refPois, res)
    reid(UserId("user1")).success shouldBe true
    reid(UserId("user2")).success shouldBe false
    reid(UserId("user3")).success shouldBe false

    reid(UserId("user2")).bestMatch.get shouldBe UserId("user3")
    reid(UserId("user3")).bestMatch.get shouldBe UserId("user2")
  }
}*/