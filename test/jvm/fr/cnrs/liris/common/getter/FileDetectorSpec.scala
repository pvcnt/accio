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

package fr.cnrs.liris.common.getter

import java.nio.file.{Path, Paths}

import fr.cnrs.liris.common.util.OS
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[FileDetectorSpec]].
 */
class FileDetectorSpec extends UnitSpec {
  private[this] val detector = new FileDetector

  behavior of "FileDetector"

  it should "detect absolute file URIs" in {
    if (OS.Current == OS.Windows) {
      assertOk("/foo", Paths.get("/tmp"), "file:///foo", withoutPwd = true)
      assertOk("C:\\", Paths.get("/tmp"), "file://C:/", withoutPwd = true)
      assertOk("C:\\?bar=baz", Paths.get("/tmp"), "file://C:/?bar=baz", withoutPwd = true)
    } else {
      assertOk("/foo", Paths.get("/tmp"), "file:///foo", withoutPwd = true)
      assertOk("/foo?bar=baz", Paths.get("/tmp"), "file:///foo?bar=baz", withoutPwd = true)
    }
  }

  it should "detect relative file URIs" in {
    assertOk("./foo", Paths.get("/tmp"), "file:///tmp/foo", withoutPwd = false)
    assertOk("./foo?foo=bar", Paths.get("/tmp"), "file:///tmp/foo?foo=bar", withoutPwd = false)
    assertOk("foo", Paths.get("/tmp"), "file:///tmp/foo", withoutPwd = false)
  }

  it should "reject relative file URIs without pwd" in {
    assertNoPwdException("./foo")
    assertNoPwdException("foo")
  }

  private def assertOk(in: String, pwd: Path, out: String, withoutPwd: Boolean) = {
    var uri = detector.detect(in, Some(pwd))
    uri should not be empty
    uri.get.getter shouldBe None
    uri.get.rawUri.toString shouldBe out
    uri.get.subdir shouldBe None

    if (withoutPwd) {
      uri = detector.detect(in, None)
      uri should not be empty
      uri.get.getter shouldBe None
      uri.get.rawUri.toString shouldBe out
      uri.get.subdir shouldBe None
    }
  }

  private def assertNoPwdException(in: String) = {
    val e = intercept[DetectorException] {
      detector.detect(in, None)
    }
    e.getMessage shouldBe "Relative paths require a pwd"
  }
}