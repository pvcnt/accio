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

package fr.cnrs.liris.common.util

import java.nio.file.Files

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[FileUtils]].
 */
class FileUtilsSpec extends UnitSpec {
  "FileUtils::safeDelete" should "delete a directory" in {
    val dir = Files.createTempDirectory("FileUtilsSpec-")
    dir.toFile.deleteOnExit()
    dir.toFile.exists() shouldBe true

    FileUtils.safeDelete(dir)
    dir.toFile.exists() shouldBe false
  }

  it should "delete a file" in {
    val file  = Files.createTempFile("FileUtilsSpec-", ".txt")
    file.toFile.deleteOnExit()
    file.toFile.exists() shouldBe true

    FileUtils.safeDelete(file)
    file.toFile.exists() shouldBe false
  }

  it should "recursively delete a directory" in {
    val dir = Files.createTempDirectory("FileUtilsSpec-")
    Files.createDirectory(dir.resolve("foo"))
    Files.createDirectory(dir.resolve("bar"))
    Files.createFile(dir.resolve("foo/foo.txt"))
    Files.createFile(dir.resolve("foo/foo2.txt"))
    dir.toFile.exists() shouldBe true
    dir.toFile.deleteOnExit()

    FileUtils.safeDelete(dir)
    dir.toFile.exists() shouldBe false
  }

  "FileUtils::expand" should "replace an initial '~/' with the home directory" in {
    FileUtils.expand("~/foo/bar") shouldBe sys.props("user.home") + "/foo/bar"
    FileUtils.expand("~foo/bar") shouldBe "~foo/bar"
    FileUtils.expand("abc/~foo/bar") shouldBe "abc/~foo/bar"
  }

  it should "replace an initial './' with the current directory" in {
    FileUtils.expand("./foo/bar") shouldBe sys.props("user.dir") + "/foo/bar"
    FileUtils.expand(".foo/bar") shouldBe ".foo/bar"
    FileUtils.expand("abc/./foo/bar") shouldBe "abc/./foo/bar"
  }
}