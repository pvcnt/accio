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

package fr.cnrs.liris.accio.framework.dsl

import fr.cnrs.liris.dal.core.api.{AtomicType, DataType, Values}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[RangeExploration]].
 */
class RangeExplorationSpec extends UnitSpec {
  behavior of "RangeExploration"

  it should "expand integers" in {
    val explo = RangeExploration(from = 10, to = 15, step = 1)
    explo.expand(DataType(AtomicType.Integer)) should contain theSameElementsAs
      Set(10, 11, 12, 13, 14, 15).map(Values.encodeInteger)
  }

  it should "expand longs" in {
    val explo = RangeExploration(from = 10000000000L, to = 10000000005L, step = 1)
    explo.expand(DataType(AtomicType.Long)) should contain theSameElementsAs
      Set(10000000000L, 10000000001L, 10000000002L, 10000000003L, 10000000004L, 10000000005L).map(Values.encodeLong)
  }

  it should "expand log10 doubles" in {
    val explo1 = RangeExploration(from = 10, to = 10000, step = 10, log10 = true)
    explo1.expand(DataType(AtomicType.Double)) should contain theSameElementsAs Set(10d, 100d, 1000d, 10000d).map(Values.encodeDouble)

    val explo2 = RangeExploration(from = 0.00001, to = 1, step = 10, log10 = true)
    explo2.expand(DataType(AtomicType.Double)) should contain theSameElementsAs Set(0.00001, 0.0001, 0.001, 0.01, 0.1, 1).map(Values.encodeDouble)
  }

  it should "not support locations" in {
    assertUnsupported(DataType(AtomicType.Location))
  }

  it should "not support datasets" in {
    assertUnsupported(DataType(AtomicType.Dataset))
  }

  it should "not support lists" in {
    assertUnsupported(DataType(AtomicType.List, Seq(AtomicType.Double)))
  }

  it should "not support sets" in {
    assertUnsupported(DataType(AtomicType.Set, Seq(AtomicType.Dataset)))
  }

  it should "not support maps" in {
    assertUnsupported(DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Integer)))
  }

  private def assertUnsupported(kind: DataType) = {
    an[IllegalArgumentException] shouldBe thrownBy {
      RangeExploration(null, null, null).expand(kind)
    }
  }
}