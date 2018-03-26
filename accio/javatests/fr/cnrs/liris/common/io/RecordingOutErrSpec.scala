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

import java.io.PrintWriter

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[RecordingOutErr]].
 */
class RecordingOutErrSpec extends UnitSpec {
  behavior of "RecordingOutErr"

  it should "record output" in {
    val outErr = RecordingOutErr()

    outErr.printOut("Test")
    outErr.printOutLn("out1")
    val outWriter = new PrintWriter(outErr.out)
    outWriter.println("Testout2")
    outWriter.flush()

    outErr.printErr("Test")
    outErr.printErrLn("err1")
    val errWriter = new PrintWriter(outErr.err)
    errWriter.println("Testerr2")
    errWriter.flush()

    outErr.outAsLatin1 shouldBe "Testout1\nTestout2\n"
    outErr.errAsLatin1 shouldBe "Testerr1\nTesterr2\n"
    outErr.hasRecordedOutput shouldBe true

    outErr.reset()
    outErr.outAsLatin1 shouldBe ""
    outErr.errAsLatin1 shouldBe ""
    outErr.hasRecordedOutput shouldBe false
  }
}