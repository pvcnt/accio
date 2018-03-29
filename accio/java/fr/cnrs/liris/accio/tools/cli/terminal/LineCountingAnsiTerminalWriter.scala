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

/**
 * Class providing the AnsiTerminalWriter interface from a terminal while additionally counting the number of
 * written lines.
 */
class LineCountingAnsiTerminalWriter(terminal: AnsiTerminal) extends AnsiTerminalWriter {
  private[this] var lineCount = 0

  override def append(text: String): AnsiTerminalWriter = {
    terminal.writeString(text)
    this
  }

  override def newline(): AnsiTerminalWriter = {
    terminal.cr()
    terminal.writeString("\n")
    lineCount += 1
    this
  }

  override def okStatus(): AnsiTerminalWriter = {
    terminal.textGreen()
    this
  }

  override def failStatus(): AnsiTerminalWriter = {
    terminal.textRed()
    terminal.textBold()
    this
  }

  override def normal(): AnsiTerminalWriter = {
    terminal.resetTerminal()
    this
  }

  def writtenLines: Int = lineCount
}