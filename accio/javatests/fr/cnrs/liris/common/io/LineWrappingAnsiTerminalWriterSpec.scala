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

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[LineWrappingAnsiTerminalWriter]].
 */
class LineWrappingAnsiTerminalWriterSpec extends UnitSpec {
  behavior of "LineWrappingAnsiTerminalWriter"
  
  import LoggingTerminalWriter._

  it should "wrap a simple new line" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 5, '+').append("abcdefghij")
    terminal.transcript shouldBe "abcd+" + NEWLINE + "efgh+" + NEWLINE + "ij"
  }

  it should "always wrap" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 5, '+').append("12345").newline();
    terminal.transcript shouldBe "1234+" + NEWLINE + "5" + NEWLINE
  }

  it should "wrap late" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 5, '+').append("1234")
    // Lines are only wrapped, once a character is written that cannot fit in the current line, and
    // not already once the last usable character of a line is used. Hence, in this example, we do
    // not want to see the continuation character.
    terminal.transcript shouldBe "1234"
  }

  it should "translate new lines" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 80, '+').append("foo\nbar\n")
    terminal.transcript shouldBe "foo" + NEWLINE + "bar" + NEWLINE
  }

  it should "reset count on new line" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 5, '+')
      .append("123")
      .newline()
      .append("abc")
      .newline()
      .append("ABC\nABC")
      .newline()
    terminal.transcript shouldBe "123" + NEWLINE + "abc" + NEWLINE + "ABC" + NEWLINE + "ABC" + NEWLINE
  }

  it should "pass through events" in {
    val terminal = new LoggingTerminalWriter
    new LineWrappingAnsiTerminalWriter(terminal, 80, '+')
      .okStatus()
      .append("ok")
      .failStatus()
      .append("fail")
      .normal()
      .append("normal")
    terminal.transcript shouldBe OK + "ok" + FAIL + "fail" + NORMAL + "normal"
  }
}
