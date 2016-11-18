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

import java.nio.file.Files

import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GlobExploration]].
 */
class GlobExplorationSpec extends UnitSpec {
  behavior of "GlobExploration"

  it should "expand into a range of strings" in {
    val dirPath = Files.createTempDirectory("accio-test-")
    Files.createFile(dirPath.resolve("foo.txt"))
    Files.createFile(dirPath.resolve("bar.txt"))
    Files.createFile(dirPath.resolve("bar.csv"))
    val uri = dirPath.toAbsolutePath.toString

    val explo1 = GlobExploration(s"$uri/*")
    explo1.expand(DataType.String) should contain theSameElementsAs Set(s"$uri/foo.txt", s"$uri/bar.txt", s"$uri/bar.csv")

    val explo2 = GlobExploration(s"$uri/bar.*")
    explo2.expand(DataType.String) should contain theSameElementsAs Set(s"$uri/bar.txt", s"$uri/bar.csv")

    val explo3 = GlobExploration(s"$uri/*.txt")
    explo3.expand(DataType.String) should contain theSameElementsAs Set(s"$uri/foo.txt", s"$uri/bar.txt")

    val explo4 = GlobExploration(s"$uri/foobar")
    explo4.expand(DataType.String) should have size 0

    FileUtils.safeDelete(dirPath)
  }

  it should "not support bytes" in {
    assertUnsupported(DataType.Byte)
  }

  it should "not support shorts" in {
    assertUnsupported(DataType.Short)
  }

  it should "not support integers" in {
    assertUnsupported(DataType.Integer)
  }

  it should "not support longs" in {
    assertUnsupported(DataType.Long)
  }

  it should "not support doubles" in {
    assertUnsupported(DataType.Double)
  }

  it should "not support booleans" in {
    assertUnsupported(DataType.Boolean)
  }

  it should "not support timestamps" in {
    assertUnsupported(DataType.Timestamp)
  }

  it should "not support durations" in {
    assertUnsupported(DataType.Duration)
  }

  it should "not support locations" in {
    assertUnsupported(DataType.Location)
  }

  it should "not support distances" in {
    assertUnsupported(DataType.Distance)
  }

  it should "not support datasets" in {
    assertUnsupported(DataType.Dataset)
  }

  it should "not support images" in {
    assertUnsupported(DataType.Image)
  }

  it should "not support lists" in {
    assertUnsupported(DataType.List(DataType.String))
  }

  it should "not support sets" in {
    assertUnsupported(DataType.Set(DataType.String))
  }

  it should "not support maps" in {
    assertUnsupported(DataType.Map(DataType.String, DataType.String))
  }

  private def assertUnsupported(kind: DataType) = {
    val expected = intercept[IllegalArgumentException] {
      GlobExploration(null).expand(kind)
    }
    expected.getMessage shouldBe s"Cannot glob with: $kind"
  }
}