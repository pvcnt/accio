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

package fr.cnrs.liris.accio.framework.filesystem.archive

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Path}

import com.google.common.io.Resources
import fr.cnrs.liris.testing.{UnitSpec, WithTmpDirectory}
import org.apache.commons.compress.utils.IOUtils

abstract class ArchiveFormatSpec extends UnitSpec with WithTmpDirectory {
  protected def createFormat: ArchiveFormat

  protected def copyResource(resourceName: String): Path = {
    val dst = tmpDir.resolve(resourceName.split("/").last)
    val in = Resources.asByteSource(Resources.getResource(resourceName)).openBufferedStream()
    val out = new BufferedOutputStream(new FileOutputStream(dst.toFile))
    try {
      IOUtils.copy(in, out)
      dst
    } finally {
      try {
        in.close()
      } finally {
        out.close()
      }
    }
  }
}

abstract class FileArchiveFormatSpec extends ArchiveFormatSpec {
  it should "decompress a single file" in {
    val format = createFormat
    val src = copyResource(singleFileArchivePath)
    val dst = tmpDir.resolve("result")
    format.decompress(src, dst)
    assertSingleFile(dst)
  }

  protected def singleFileArchivePath: String

  it should "compress a single file" in {
    val format = createFormat
    val src = tmpDir.resolve("foobar.txt")
    Files.write(src, "foobar\n".getBytes)
    val compressed = tmpDir.resolve("foobar.txt.compressed")
    format.compress(src, compressed)
    val decompressed = tmpDir.resolve("foobar.txt.orig")
    format.decompress(compressed, decompressed)
    assertSingleFile(decompressed)
  }

  private def assertSingleFile(dst: Path) = {
    dst.toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst)) shouldBe "foobar\n"
  }
}

abstract class CompressedArchiveFormatSpec extends ArchiveFormatSpec {
  it should "decompress a single file" in {
    val format = createFormat
    val src = copyResource(singleFileArchivePath)
    val dst = tmpDir.resolve("result")
    format.decompress(src, dst)
    assertSingleFile(dst)
  }

  protected def singleFileArchivePath: String

  it should "compress a single file" in {
    val format = createFormat
    val src = tmpDir.resolve("foobar.txt")
    Files.write(src, "foobar\n".getBytes)
    val compressed = tmpDir.resolve("foobar.txt.compressed")
    format.compress(src, compressed)
    val dst = tmpDir.resolve("result")
    format.decompress(compressed, dst)
    assertSingleFile(dst)
  }

  it should "decompress a tree" in {
    val format = createFormat
    val src = copyResource(treeArchivePath)
    val dst = tmpDir.resolve("result")
    format.decompress(src, dst)
    assertTree(dst)
  }

  protected def treeArchivePath: String

  it should "compress a tree" in {
    val format = createFormat
    val src = tmpDir.resolve("source")
    Files.createDirectory(src)
    Files.createDirectory(src.resolve("foo"))
    Files.createDirectory(src.resolve("bar"))
    Files.write(src.resolve("foobar.txt"), "foobar\n".getBytes)
    Files.write(src.resolve("foo/foo.txt"), "foo\n".getBytes)
    Files.write(src.resolve("foo/bar.txt"), "bar\n".getBytes)
    Files.write(src.resolve("bar/foo.txt"), "foo\n".getBytes)
    Files.write(src.resolve("bar/bar.txt"), "bar\n".getBytes)
    val compressed = tmpDir.resolve("result")
    format.compress(src, compressed)
    val decompressed = tmpDir.resolve("orig")
    format.decompress(compressed, decompressed)
    assertTree(decompressed)
  }

  private def assertSingleFile(dst: Path) = {
    dst.toFile.isDirectory shouldBe true
    new String(Files.readAllBytes(dst.resolve("foobar.txt"))) shouldBe "foobar\n"
  }

  private def assertTree(dst: Path) = {
    dst.resolve("foobar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foobar.txt"))) shouldBe "foobar\n"
    dst.resolve("foo/foo.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foo/foo.txt"))) shouldBe "foo\n"
    dst.resolve("foo/bar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("foo/bar.txt"))) shouldBe "bar\n"
    dst.resolve("bar/foo.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("bar/foo.txt"))) shouldBe "foo\n"
    dst.resolve("bar/bar.txt").toFile.isFile shouldBe true
    new String(Files.readAllBytes(dst.resolve("bar/bar.txt"))) shouldBe "bar\n"
  }
}