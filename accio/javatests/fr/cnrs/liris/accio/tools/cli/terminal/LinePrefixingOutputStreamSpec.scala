// Code has been translated from Bazel, subject to the following license:
/**
 * Copyright 2014 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cnrs.liris.accio.tools.cli.terminal

import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.common.base.Charsets
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[LinePrefixingOutputStream]].
 */
class LinePrefixingOutputStreamSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "LinePrefixingOutputStream"

  private def bytes(string: String): Array[Byte] = string.getBytes(Charsets.UTF_8)

  private def string(bytes: Array[Byte]): String = new String(bytes, Charsets.UTF_8)

  private var out : ByteArrayOutputStream = null
  private var prefixOut: OutputStream = null

  override protected def beforeEach() = {
    super.beforeEach()
    out = new ByteArrayOutputStream
    prefixOut = new LinePrefixingOutputStream("Prefix: ", out)
  }

  override protected def afterEach() = {
    super.afterEach()
    out = null
    prefixOut = null
  }

  it should "give no output until new line" in {
    prefixOut.write(bytes("We won't be seeing any output."))
    string(out.toByteArray) should have length 0
  }

  it should "give output if flushed" in {
    prefixOut.write(bytes("We'll flush after this line."))
    prefixOut.flush()
    string(out.toByteArray) shouldBe "Prefix: We'll flush after this line.\n"
  }

  it should "auto-flush upon new line" in {
    prefixOut.write(bytes("Hello, newline.\n"))
    string(out.toByteArray) shouldBe "Prefix: Hello, newline.\n"
  }

  it should "auto-flush upon embedded new line" in {
    prefixOut.write(bytes("Hello line1.\nHello line2.\nHello line3.\n"))
    string(out.toByteArray) shouldBe "Prefix: Hello line1.\nPrefix: Hello line2.\nPrefix: Hello line3.\n"
  }

  it should "buffer max length then flush" in {
    var junk = "lots of characters of non-newline junk. "
    while (junk.length() < LineFlushingOutputStream.BufferLength) {
      junk = junk + junk
    }
    junk = junk.substring(0, LineFlushingOutputStream.BufferLength)

    // Also test bug where write on a full buffer blows up
    prefixOut.write(bytes(junk + junk))
    prefixOut.write(bytes(junk + junk))
    prefixOut.write(bytes("x"))
    string(out.toByteArray) shouldBe "Prefix: " + junk + "\n" + "Prefix: " + junk + "\n" + "Prefix: " + junk + "\n" + "Prefix: " + junk + "\n"
  }
}