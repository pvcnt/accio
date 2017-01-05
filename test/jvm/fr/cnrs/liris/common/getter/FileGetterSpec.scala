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

import java.io.IOException
import java.net.URI
import java.nio.file.Files

import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[FileGetter]].
 */
class FileGetterSpec extends UnitSpec {

  behavior of "FileGetter"

  it should "get a file and copy it" in {
    val getter = new FileGetter(copy = true)
    val srcFile = Files.createTempFile("getter-test-", ".txt")
    Files.write(srcFile, "foobar file".getBytes)
    val dstFile = Files.createTempDirectory("getter-test-").resolve("file.txt")

    try {
      getter.get(srcFile.toUri, dstFile)
      dstFile.toFile.exists() shouldBe true
      Files.readAllBytes(dstFile) shouldBe "foobar file".getBytes
      Files.isSymbolicLink(dstFile) shouldBe false
    } finally {
      FileUtils.safeDelete(srcFile)
      FileUtils.safeDelete(dstFile.getParent)
    }
  }

  it should "get a file and symlink it" in {
    val getter = new FileGetter(copy = false)
    val srcFile = Files.createTempFile("getter-test-", ".txt")
    Files.write(srcFile, "foobar file".getBytes)
    val dstFile = Files.createTempDirectory("getter-test-").resolve("file.txt")

    try {
      getter.get(srcFile.toUri, dstFile)
      dstFile.toFile.exists() shouldBe true
      Files.readAllBytes(dstFile) shouldBe "foobar file".getBytes
      Files.isSymbolicLink(dstFile) shouldBe true
    } finally {
      FileUtils.safeDelete(srcFile)
      FileUtils.safeDelete(dstFile.getParent)
    }
  }

  it should "get a file and symlink it (overwrite)" in {
    val getter = new FileGetter(copy = false)
    val srcFile = Files.createTempFile("getter-test-", ".txt")
    Files.write(srcFile, "foobar file".getBytes)
    val dstFile = Files.createTempDirectory("getter-test-").resolve("file.txt")
    val tmpFile = Files.createTempFile("getter-test-", ".txt")
    Files.write(tmpFile, "other file".getBytes)
    Files.createSymbolicLink(dstFile, tmpFile)

    try {
      getter.get(srcFile.toUri, dstFile)
      dstFile.toFile.exists() shouldBe true
      Files.readAllBytes(dstFile) shouldBe "foobar file".getBytes
      Files.isSymbolicLink(dstFile) shouldBe true
    } finally {
      FileUtils.safeDelete(srcFile)
      FileUtils.safeDelete(dstFile.getParent)
      FileUtils.safeDelete(tmpFile)
    }
  }

  it should "reject a source directory" in {
    val getter = new FileGetter(copy = true)
    val srcDir = Files.createTempDirectory("getter-test-")
    val dstFile = Files.createTempDirectory("getter-test-").resolve("foo.txt")

    try {
      val e = intercept[IOException] {
        getter.get(srcDir.toUri, dstFile)
      }
      e.getMessage should startWith("Source must be a file: ")
    } finally {
      FileUtils.safeDelete(srcDir)
      FileUtils.safeDelete(dstFile.getParent)
    }
  }

  it should "reject a non-existent source" in {
    val getter = new FileGetter(copy = true)
    val dstFile = Files.createTempDirectory("getter-test-").resolve("foo.txt")

    try {
      val e = intercept[IOException] {
        getter.get(new URI("file:///non/existent/file"), dstFile)
      }
      e.getMessage should startWith("Source does not exist: ")
    } finally {
      FileUtils.safeDelete(dstFile.getParent)
    }
  }

  it should "reject an existent source" in {
    val getter = new FileGetter(copy = true)
    val srcFile = Files.createTempFile("getter-test-", ".txt")
    val dstFile = Files.createTempFile("getter-test-", ".txt")

    try {
      val e = intercept[IOException] {
        getter.get(srcFile.toUri, dstFile)
      }
      e.getMessage should startWith("Destination already exists: ")
    } finally {
      FileUtils.safeDelete(srcFile)
      FileUtils.safeDelete(dstFile)
    }
  }

  it should "reject a non-symlink existent source" in {
    val getter = new FileGetter(copy = false)
    val srcFile = Files.createTempFile("getter-test-", ".txt")
    val dstFile = Files.createTempFile("getter-test-", ".txt")

    try {
      val e = intercept[IOException] {
        getter.get(srcFile.toUri, dstFile)
      }
      e.getMessage should startWith("Destination already exists and is not a symlink: ")
    } finally {
      FileUtils.safeDelete(srcFile)
      FileUtils.safeDelete(dstFile)
    }
  }
}