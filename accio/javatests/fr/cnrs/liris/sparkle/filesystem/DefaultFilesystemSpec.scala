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

package fr.cnrs.liris.sparkle.filesystem

import java.io.ByteArrayOutputStream
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.io.ByteStreams
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Unit tests for [[DefaultFilesystem]].
 */
class DefaultFilesystemSpec extends UnitSpec with CreateTmpDirectory with GeneratorDrivenPropertyChecks {
  behavior of "PosixFilesystem"

  it should "write into files" in {
    val idx = new AtomicInteger()
    forAll { b: Array[Byte] =>
      val filename = s"foo${idx.getAndIncrement()}"
      val os = DefaultFilesystem.createOutputStream(s"file://$tmpDir/$filename")
      os.write(b)
      os.close()
      Files.exists(tmpDir.resolve(filename)) shouldBe true
      Files.readAllBytes(tmpDir.resolve(filename)).deep shouldBe b.deep
    }
  }

  it should "read from files" in {
    val idx = new AtomicInteger()
    forAll { b: Array[Byte] =>
      val filename = s"foo${idx.getAndIncrement()}"
      Files.write(tmpDir.resolve(filename), b)
      val is = DefaultFilesystem.createInputStream(s"file://$tmpDir/$filename")
      val baos = new ByteArrayOutputStream
      ByteStreams.copy(is, baos)
      is.close()
      baos.toByteArray.deep shouldBe b.deep
    }
  }

  it should "detect files and directories" in {
    Files.createFile(tmpDir.resolve("foo"))
    Files.createDirectory(tmpDir.resolve("bar"))

    DefaultFilesystem.isDirectory(s"file://$tmpDir/bar") shouldBe true
    DefaultFilesystem.isFile(s"file://$tmpDir/bar") shouldBe false
    DefaultFilesystem.isFile(s"file://$tmpDir/foo") shouldBe true
    DefaultFilesystem.isDirectory(s"file://$tmpDir/foo") shouldBe false
  }

  it should "list files inside directories" in {
    Files.createFile(tmpDir.resolve("foo"))
    Files.createFile(tmpDir.resolve("foobar"))
    Files.createDirectory(tmpDir.resolve("bar"))
    Files.createFile(tmpDir.resolve("bar/foo"))
    Files.createFile(tmpDir.resolve("bar/bar"))

    DefaultFilesystem.list(s"file://$tmpDir") should contain theSameElementsAs Set(
      s"file://$tmpDir/foo",
      s"file://$tmpDir/foobar",
      s"file://$tmpDir/bar/foo",
      s"file://$tmpDir/bar/bar")
  }

  it should "delete files and directories" in {
    Files.createFile(tmpDir.resolve("foo"))
    Files.createFile(tmpDir.resolve("foobar"))
    Files.createDirectory(tmpDir.resolve("bar"))
    Files.createFile(tmpDir.resolve("bar/foo"))
    Files.createFile(tmpDir.resolve("bar/bar"))

    DefaultFilesystem.delete(s"file://$tmpDir/foo")
    DefaultFilesystem.isFile(s"file://$tmpDir/foo") shouldBe false
    DefaultFilesystem.isDirectory(s"file://$tmpDir/foo") shouldBe false
    DefaultFilesystem.isFile(s"file://$tmpDir/foobar") shouldBe true

    DefaultFilesystem.delete(s"file://$tmpDir/bar")
    DefaultFilesystem.isFile(s"file://$tmpDir/bar") shouldBe false
    DefaultFilesystem.isDirectory(s"file://$tmpDir/bar") shouldBe false
    DefaultFilesystem.isFile(s"file://$tmpDir/foobar") shouldBe true
  }
}
