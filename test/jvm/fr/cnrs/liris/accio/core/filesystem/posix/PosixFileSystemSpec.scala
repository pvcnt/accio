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

package fr.cnrs.liris.accio.core.filesystem.posix

import java.io.IOException
import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.core.filesystem.FileSystem
import fr.cnrs.liris.testing.{UnitSpec, WithTmpDirectory}

/**
 * Unit tests for [[PosixFileSystem]].
 */
class PosixFileSystemSpec extends UnitSpec with WithTmpDirectory {
  behavior of "PosixFileSystem"

  it should "read a file (no symlink)" in {
    val fs = createFileSystem(symlink = false)
    val dst = tmpDir.resolve("foobar.txt")
    assertRead(fs, "foobar.txt", dst)
    Files.isSymbolicLink(dst) shouldBe false
  }

  it should "read a file (symlink)" in {
    val fs = createFileSystem(symlink = true)
    val dst = tmpDir.resolve("foobar.txt")
    assertRead(fs, "foobar.txt", dst)
    Files.isSymbolicLink(dst) shouldBe true
  }

  it should "read a file (symlink/overwrite)" in {
    val fs = createFileSystem(symlink = true)
    val dst = tmpDir.resolve("foobar.txt")
    val tmpFile = tmpDir.resolve("tmp.txt")
    Files.write(tmpFile, "other file".getBytes)
    Files.createSymbolicLink(dst, tmpFile)
    assertRead(fs, "foobar.txt", dst)
    Files.isSymbolicLink(dst) shouldBe true
  }

  private def assertRead(fs: FileSystem, src: String, dst: Path) = {
    fs.read(src, dst)
    dst.toFile.exists() shouldBe true
    Files.readAllBytes(dst) shouldBe "foobar file".getBytes
  }

  it should "reject a non-existent source when reading" in {
    val fs = createFileSystem(symlink = false)
    val dstFile = tmpDir.resolve("foobar.txt")
    val e = intercept[IOException] {
      fs.read("/non/existent/file", dstFile)
    }
    e.getMessage should startWith("Source does not exist: ")
  }

  it should "reject an existent destination when reading" in {
    val fs = createFileSystem(symlink = false)
    val dst = tmpDir.resolve("foobar.txt")
    Files.write(dst, "some content".getBytes)
    val e = intercept[IOException] {
      fs.read("foobar.txt", dst)
    }
    e.getMessage should startWith("Destination already exists: ")
  }

  it should "reject a non-symlink existent destination when reading" in {
    val fs = createFileSystem(symlink = true)
    val dst = tmpDir.resolve("foobar.txt")
    Files.write(dst, "some content".getBytes)
    val e = intercept[IOException] {
      fs.read("foobar.txt", dst)
    }
    e.getMessage should startWith("Destination already exists and is not a symlink: ")
  }

  private def createFileSystem(symlink: Boolean) = {
    val fs = new PosixFileSystem(PosixFileSystemConfig(tmpDir.resolve("data"), symlink))
    Files.createDirectory(tmpDir.resolve("data"))
    Files.write(tmpDir.resolve("data/foobar.txt"), "foobar file".getBytes)
    fs
  }
}