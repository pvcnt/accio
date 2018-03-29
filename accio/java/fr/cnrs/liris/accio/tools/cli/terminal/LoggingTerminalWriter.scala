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
 * An [[AnsiTerminalWriter]] that keeps just generates a transcript
 * of the events it was exposed of.
 */
class LoggingTerminalWriter(discardHighlight: Boolean = false) extends AnsiTerminalWriter {

  import LoggingTerminalWriter._

  // Strings for recording the non-append calls.
  private var _transcript = ""

  override def append(text: String): AnsiTerminalWriter = {
    _transcript += text
    this
  }

  override def newline(): AnsiTerminalWriter = {
    if (!discardHighlight) {
      _transcript += NEWLINE
    } else {
      _transcript += "\n"
    }
    this
  }

  override def okStatus(): AnsiTerminalWriter = {
    if (!discardHighlight) {
      _transcript += OK
    }
    this
  }

  override def failStatus(): AnsiTerminalWriter = {
    if (!discardHighlight) {
      _transcript += FAIL
    }
    this
  }

  override def normal(): AnsiTerminalWriter = {
    if (!discardHighlight) {
      _transcript += NORMAL
    }
    this
  }

  def transcript: String = _transcript
}

object LoggingTerminalWriter {
  val NEWLINE = "[NL]"
  val OK = "[OK]"
  val FAIL = "[FAIL]"
  val NORMAL = "[NORMAL]"
}