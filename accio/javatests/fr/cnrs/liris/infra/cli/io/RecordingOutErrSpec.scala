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
// https://github.com/bazelbuild/bazel/blob/master/src/test/java/com/google/devtools/build/lib/util/io/RecordingOutErrTest.java

package fr.cnrs.liris.infra.cli.io

import java.io.PrintWriter

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[RecordingOutErr]].
 */
class RecordingOutErrSpec extends UnitSpec {
  behavior of "RecordingOutErr"

  it should "record out and err" in {
    val outErr = RecordingOutErr()

    outErr.printOut("Test")
    outErr.printOutLn("out1")
    var writer = new PrintWriter(outErr.out)
    writer.println("Testout2")
    writer.flush()

    outErr.printErr("Test")
    outErr.printErrLn("err1")
    writer = new PrintWriter(outErr.err)
    writer.println("Testerr2")
    writer.flush()

    outErr.outAsLatin1 shouldBe "Testout1\nTestout2\n"
    outErr.errAsLatin1 shouldBe "Testerr1\nTesterr2\n"

    outErr.hasRecordedOutput shouldBe true

    outErr.reset()

    outErr.outAsLatin1 should have size 0
    outErr.errAsLatin1 should have size 0
    outErr.hasRecordedOutput shouldBe false
  }
}
