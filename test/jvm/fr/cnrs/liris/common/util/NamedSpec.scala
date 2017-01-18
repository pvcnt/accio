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

package fr.cnrs.liris.common.util

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[Named]].
 */
class NamedSpec extends UnitSpec {
  behavior of "Named"

  it should "provide consistent equality" in {
    val firstFoo = new FirstNamed("foo")
    val firstBar = new FirstNamed("bar")
    val otherFirstFoo = new FirstNamed("foo")
    val secondFoo = new RandomClass("foo")
    val randomFoo = new RandomClass("foo")

    firstFoo should equal(otherFirstFoo)
    firstFoo shouldNot equal(firstBar)
    firstFoo shouldNot equal(randomFoo)
    firstFoo shouldNot equal(secondFoo)
  }

  it should "provide consistent hash code" in {
    val firstFoo = new FirstNamed("foo")
    val firstBar = new FirstNamed("bar")
    val otherFirstFoo = new FirstNamed("foo")
    val randomFoo = new RandomClass("foo")

    firstFoo.hashCode shouldBe otherFirstFoo.hashCode
    firstFoo.hashCode shouldNot be(firstBar.hashCode)
    firstFoo.hashCode shouldNot be(randomFoo.hashCode)
  }

  class FirstNamed(override val name: String) extends Named

  class SecondNamed(override val name: String) extends Named

  class RandomClass(val name: String)

}