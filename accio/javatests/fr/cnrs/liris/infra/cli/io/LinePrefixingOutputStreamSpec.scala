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

// Code has been translated from Bazel, subject to the Apache License, Version 2.0.
// https://github.com/bazelbuild/bazel/blob/master/src/test/java/com/google/devtools/build/lib/util/io/LinePrefixingOutputStreamTest.java

package fr.cnrs.liris.infra.cli.io

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[LinePrefixingOutputStream]] (and [[LineFlushingOutputStream]]).
 */
class LinePrefixingOutputStreamSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "LinePrefixingOutputStream"

  private var out: ByteArrayOutputStream = _
  private var prefixOut: LinePrefixingOutputStream = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    out = new ByteArrayOutputStream
    prefixOut = new LinePrefixingOutputStream("Prefix: ", out)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    prefixOut.close()
    prefixOut = null
    out = null
  }

  it should "not output until new line" in {
    prefixOut.write(bytes("We won't be seeing any output."))
    out.toByteArray should have size 0
  }

  it should "output if flushed" in {
    prefixOut.write(bytes("We'll flush after this line."))
    prefixOut.flush()
    string(out.toByteArray) shouldBe "Prefix: We'll flush after this line.\n"
  }

  it should "auto flush upon new line" in {
    prefixOut.write(bytes("Hello, newline.\n"))
    string(out.toByteArray) shouldBe "Prefix: Hello, newline.\n"
  }

  it should "auto flush upon embedded new line" in {
    prefixOut.write(bytes("Hello line1.\nHello line2.\nHello line3.\n"))
    string(out.toByteArray) shouldBe "Prefix: Hello line1.\nPrefix: Hello line2.\nPrefix: Hello line3.\n"
  }

  it should "buffer max length before flush" in {
    var junk = "lots of characters of non-newline junk. "
    while (junk.length() < LineFlushingOutputStream.BufferLength) {
      junk = junk + junk
    }
    junk = junk.substring(0, LineFlushingOutputStream.BufferLength)

    // Also test bug where write on a full buffer blows up.
    prefixOut.write(bytes(junk + junk))
    prefixOut.write(bytes(junk + junk))
    prefixOut.write(bytes("x"))
    string(out.toByteArray) shouldBe "Prefix: " + junk + "\nPrefix: " + junk + "\nPrefix: " + junk + "\nPrefix: " + junk + "\n"
  }

  private def bytes(str: String): Array[Byte] = str.getBytes(UTF_8)

  private def string(bytes: Array[Byte]): String = new String(bytes, UTF_8)
}
