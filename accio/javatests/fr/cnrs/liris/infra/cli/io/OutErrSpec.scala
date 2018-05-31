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
// https://github.com/bazelbuild/bazel/blob/master/src/test/java/com/google/devtools/build/lib/util/io/OutErrTest.java

package fr.cnrs.liris.infra.cli.io

import java.io.ByteArrayOutputStream

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[OutErr]].
 */
class OutErrSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "OutErr"

  private var out: ByteArrayOutputStream = _
  private var err: ByteArrayOutputStream = _
  private var outErr: OutErr = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    out = new ByteArrayOutputStream
    err = new ByteArrayOutputStream
    outErr = new OutErr(out, err)
  }

  override def afterEach(): Unit = {
    super.afterEach()
    outErr.close()
    outErr = null
    out = null
    err = null
  }

  it should "retain out and err" in {
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
