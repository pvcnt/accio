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

package fr.cnrs.liris.lumos.domain

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DataType]].
 */
class DataTypeSpec extends UnitSpec {
  behavior of "DataType"

  it should "retrieve the value of" in {
    DataType.values should have size 8 // This forces to update the test when we add a new data type.
    DataType.valueOf("Int") shouldBe Some(DataType.Int)
    DataType.valueOf("Long") shouldBe Some(DataType.Long)
    DataType.valueOf("Float") shouldBe Some(DataType.Float)
    DataType.valueOf("Double") shouldBe Some(DataType.Double)
    DataType.valueOf("String") shouldBe Some(DataType.String)
    DataType.valueOf("Bool") shouldBe Some(DataType.Bool)
    DataType.valueOf("Dataset") shouldBe Some(DataType.Dataset)
    DataType.valueOf("File") shouldBe Some(DataType.File)
    DataType.valueOf("foobar") shouldBe None
  }
}
