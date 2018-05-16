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

import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.{LatLng, Point}

/**
 * Unit tests for [[Event]].
 */
class EventSpec extends UnitSpec with WithTraceGenerator {
  behavior of "Event"

  it should "provide a copy with a different location" in {
    val event = Event(Me, Here, Now)
    event.withPoint(Point(3, 42)).point shouldBe Point(3, 42)
    event.withLatLng(LatLng.degrees(37.43, -120)).latLng shouldBe LatLng.degrees(37.43, -120)
  }

  it should "be chronologically comparable" in {
    val event1 = Event(Me, Here, Now)
    val event2 = Event(Me, Here, Now.minus(1000))
    event1 shouldBe > (event2)
  }

  it should "get the user" in {
    Event("me", Here, Now).user shouldBe "me"
    Event("me-01", Here, Now).user shouldBe "me"
    Event("me-01-02", Here, Now).user shouldBe "me"
  }
}