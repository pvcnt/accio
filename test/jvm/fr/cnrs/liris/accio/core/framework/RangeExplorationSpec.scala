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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[RangeExploration]].
 */
class RangeExplorationSpec extends UnitSpec {
  behavior of "RangeExploration"

  it should "expand into a range of integers" in {
    val explo = RangeExploration(from = 10, to = 15, step = 1)
    explo.expand(DataType.Integer) should contain theSameElementsAs Set(10, 11, 12, 13, 14, 15)
  }

  it should "expand into a range of longs" in {
    val explo = RangeExploration(from = 10000000000L, to = 10000000005L, step = 1)
    explo.expand(DataType.Long) should contain theSameElementsAs Set(10000000000L, 10000000001L, 10000000002L, 10000000003L, 10000000004L, 10000000005L)
  }

  it should "expand into a log10 range of doubles" in {
    val explo = RangeExploration(from = 10, to = 10000, step = 10, log10 = true)
    explo.expand(DataType.Double) should contain theSameElementsAs Set(10, 100, 1000, 10000)
  }
}
