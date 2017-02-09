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

package fr.cnrs.liris.accio.core.dsl

import fr.cnrs.liris.dal.core.api.{AtomicType, DataType, Values}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[SingletonExploration]].
 */
class SingletonExplorationSpec extends UnitSpec {
  behavior of "SingletonExploration"

  it should "expand bytes" in {
    val explo = SingletonExploration(1.toByte)
    explo.expand(DataType(AtomicType.Byte)) should contain theSameElementsAs Set(Values.encodeByte(1))
  }

  it should "expand integers" in {
    val explo = SingletonExploration(10)
    explo.expand(DataType(AtomicType.Integer)) should contain theSameElementsAs Set(Values.encodeInteger(10))
  }

  it should "expand doubles" in {
    val explo = SingletonExploration(42)
    explo.expand(DataType(AtomicType.Double)) should contain theSameElementsAs Set(Values.encodeDouble(42))
  }

  it should "expand longs" in {
    val explo = SingletonExploration(103212032002347323L)
    explo.expand(DataType(AtomicType.Long)) should contain theSameElementsAs Set(Values.encodeLong(103212032002347323L))
  }

  it should "expand booleans" in {
    var explo = SingletonExploration(false)
    explo.expand(DataType(AtomicType.Boolean)) should contain theSameElementsAs Set(Values.encodeBoolean(false))

    explo = SingletonExploration(true)
    explo.expand(DataType(AtomicType.Boolean)) should contain theSameElementsAs Set(Values.encodeBoolean(true))
  }

  it should "expand strings" in {
    val explo = SingletonExploration("foo bar")
    explo.expand(DataType(AtomicType.String)) should contain theSameElementsAs Set(Values.encodeString("foo bar"))
  }
}