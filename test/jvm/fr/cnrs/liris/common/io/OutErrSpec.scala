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

package fr.cnrs.liris.common.io

import java.io.ByteArrayOutputStream

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[OutErr]].
 */
class OutErrSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "OutErr"

  private var out: ByteArrayOutputStream = null
  private var err: ByteArrayOutputStream = null
  private var outErr: OutErr = null

  override protected def beforeEach() = {
    out = new ByteArrayOutputStream
    err = new ByteArrayOutputStream
    outErr = new OutErr(out, err)
  }

  override protected def afterEach() = {
    out = null
    err = null
    outErr = null
  }

  it should "retain out/err" in {
    outErr.out shouldBe theSameInstanceAs(out)
    outErr.err shouldBe theSameInstanceAs(err)
  }

  it should "print to out" in {
    outErr.printOut("Hello, world.")
    new String(out.toByteArray) shouldBe "Hello, world."
  }

  it should "print to err" in {
    outErr.printErr("Hello, moon.")
    new String(err.toByteArray) shouldBe "Hello, moon."
  }

  it should "print to out with a new line" in {
    outErr.printOutLn("With a newline.")
    new String(out.toByteArray) shouldBe "With a newline.\n"
  }

  it should "print to err with a new line" in {
    outErr.printErrLn("With a newline.")
    new String(err.toByteArray) shouldBe "With a newline.\n"
  }

  it should "print two lines to out" in {
    outErr.printOutLn("line 1")
    outErr.printOutLn("line 2")
    new String(out.toByteArray) shouldBe "line 1\nline 2\n"
  }

  it should "print two lines to err" in {
    outErr.printErrLn("line 1")
    outErr.printErrLn("line 2")
    new String(err.toByteArray) shouldBe "line 1\nline 2\n"
  }
}