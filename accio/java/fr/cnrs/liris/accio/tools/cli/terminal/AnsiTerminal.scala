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

import java.io.OutputStream

/**
 * A class which encapsulates the fancy curses-type stuff that you can do using
 * standard ANSI terminal control sequences.
 *
 * @param out Output stream to wrap which is going to be displayed in an ANSI compatible terminal or shell window.
 */
class AnsiTerminal(out: OutputStream) {
  /**
   * Moves the cursor upwards by a specified number of lines. This will not
   * cause any scrolling if it tries to move above the top of the terminal
   * window.
   */
  def cursorUp(numLines: Int): Unit = {
    writeBytes(AnsiTerminal.ESC, ("" + numLines).getBytes(), Array[Byte](AnsiTerminal.UP))
  }

  /**
   * Clear the current terminal line from the cursor position to the end.
   */
  def clearLine(): Unit = {
    writeEscapeSequence(AnsiTerminal.ERASE_LINE)
  }

  /**
   * Makes any text output to the terminal appear in bold.
   */
  def textBold(): Unit = {
    writeEscapeSequence(AnsiTerminal.TEXT_BOLD, AnsiTerminal.SET_GRAPHICS)
  }

  /**
   * Set the color of the foreground or background of the terminal.
   *
   * @param color one of the foreground or background color
   *              constants
   */
  def setTextColor(color: String): Unit = {
    writeBytes(AnsiTerminal.ESC, color.getBytes(), Array[Byte](AnsiTerminal.SET_GRAPHICS))
  }

  /**
   * Resets the terminal colors and fonts to defaults.
   */
  def resetTerminal(): Unit = writeEscapeSequence('0', 'm')

  /**
   * Makes text print on the terminal in red.
   */
  def textRed(): Unit = setTextColor(AnsiTerminal.FG_RED)

  /**
   * Makes text print on the terminal in blue.
   */
  def textBlue(): Unit = setTextColor(AnsiTerminal.FG_BLUE)

  /**
   * Makes text print on the terminal in red.
   */
  def textGreen(): Unit = setTextColor(AnsiTerminal.FG_GREEN)

  /**
   * Makes text print on the terminal in red.
   */
  def textMagenta(): Unit = setTextColor(AnsiTerminal.FG_MAGENTA)

  /**
   * Set the terminal title.
   *
   * @param title New terminal title.
   */
  def setTitle(title: String): Unit = writeBytes(AnsiTerminal.SET_TERM_TITLE, title.getBytes, Array(AnsiTerminal.BEL))

  /**
   * Writes a string to the terminal using the current font, color and cursor
   * position settings.
   *
   * @param text the text to write
   */
  def writeString(text: String): Unit = out.write(text.getBytes)

  /**
   * Writes a byte sequence to the terminal using the current font, color and
   * cursor position settings.
   *
   * @param bytes the bytes to write
   */
  def writeBytes(bytes: Array[Byte]): Unit = out.write(bytes)

  /**
   * Utility method which makes it easier to generate the control sequences for
   * the terminal.
   *
   * @param bytes bytes which should be prefixed with the terminal escape
   *              sequence to produce a valid control sequence
   */
  private def writeEscapeSequence(bytes: Byte*): Unit = writeBytes(AnsiTerminal.ESC, bytes.toArray)

  /**
   * Utility method for generating control sequences. Takes a collection of byte
   * arrays, which contain the components of a control sequence, concatenates
   * them, and prints them to the terminal.
   *
   * @param stuff the byte arrays that make up the sequence to be sent to the terminal
   */
  private def writeBytes(stuff: Array[Byte]*): Unit = stuff.foreach(out.write)

  /**
   * Sends a carriage return to the terminal.
   */
  def cr(): Unit = writeBytes(AnsiTerminal.CR)

  /**
   * Flushes the underlying stream.
   * This class does not do any buffering of its own, but the underlying
   * OutputStream may.
   */
  def flush(): Unit = out.flush()
}

object AnsiTerminal {
  private val ESC = Array[Byte](27, '[')
  private val BEL = 7.toByte
  private val UP = 'A'.toByte
  private val ERASE_LINE = 'K'.toByte
  private val SET_GRAPHICS = 'm'.toByte
  private val TEXT_BOLD = '1'.toByte
  private val SET_TERM_TITLE = Array[Byte](27, ']', '0', ';')

  val FG_BLACK = "30"
  val FG_RED = "31"
  val FG_GREEN = "32"
  val FG_YELLOW = "33"
  val FG_BLUE = "34"
  val FG_MAGENTA = "35"
  val FG_CYAN = "36"
  val FG_GRAY = "37"
  val BG_BLACK = "40"
  val BG_RED = "41"
  val BG_GREEN = "42"
  val BG_YELLOW = "43"
  val BG_BLUE = "44"
  val BG_MAGENTA = "45"
  val BG_CYAN = "46"
  val BG_GRAY = "47"

  val CR = Array[Byte](13)
}