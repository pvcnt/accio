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

import java.io.IOException

/**
 * An append-only interface to access to a terminal.
 *
 * This interface allows to specify a text to be written to the user
 * using semantical highlighting (like failure) without any knowledge
 * about the nature of the terminal or the position the text is to be
 * written to. Callers to this interface should make no assumption
 * about how the text is rendered; if the user's terminal does not
 * support, e.g., colors, the highlighting might be done by adding
 * additional characters.
 *
 * All interface functions are supposed to return the object itself
 * to allow chaining of commands.
 */
trait AnsiTerminalWriter {
  /**
   * Write some text to the user
   */
  @throws[IOException]
  def append(text: String ): AnsiTerminalWriter

  /**
   * Start a new line in the way appropriate for the given terminal
   */
  @throws[IOException]
  def newline(): AnsiTerminalWriter

  /**
   * Tell the terminal that the following text will be a positive
   * status message.
   */
  @throws[IOException]
  def okStatus(): AnsiTerminalWriter

  /**
   * Tell the terminal that the following text will be an error-reporting
   * status message.
   */
  @throws[IOException]
  def failStatus(): AnsiTerminalWriter

  /**
   * Tell the terminal that the following text will be normal text, not
   * indicating a status or similar.
   */
  @throws[IOException]
  def normal(): AnsiTerminalWriter
}