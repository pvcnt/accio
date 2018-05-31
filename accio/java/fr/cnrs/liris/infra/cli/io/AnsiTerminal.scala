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
// https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/io/AnsiTerminal.java

package fr.cnrs.liris.infra.cli.io

import java.io.OutputStream

/**
 * A class which encapsulates the fancy curses-type stuff that you can do using standard ANSI
 * terminal control sequences.
 *
 * @param out Output stream to wrap which is going to be displayed in an ANSI compatible terminal
 *            or shell window.
 */
final class AnsiTerminal(out: OutputStream) {

  import AnsiTerminal._

  /**
   * Moves the cursor upwards by a specified number of lines. This will not cause any scrolling
   * if it tries to move above the top of the terminal window.
   */
  def cursorUp(numLines: Int): Unit = writeBytes(ESC, ("" + numLines).getBytes(), Array[Byte](UP))

  /**
   * Clear the current terminal line from the cursor position to the end.
   */
  def clearLine(): Unit = writeEscapeSequence(ERASE_LINE)

  /**
   * Makes any text output to the terminal appear in bold.
   */
  def textBold(): Unit = writeEscapeSequence(TEXT_BOLD, SET_GRAPHICS)

  /**
   * Set the color of the foreground or background of the terminal.
   *
   * @param color One of the foreground or background color constants.
   */
  def setTextColor(color: String): Unit = {
    writeBytes(ESC, color.getBytes(), Array[Byte](SET_GRAPHICS))
  }

  /**
   * Resets the terminal colors and fonts to defaults.
   */
  def resetTerminal(): Unit = writeEscapeSequence('0', 'm')

  /**
   * Makes text print on the terminal in red.
   */
  def textRed(): Unit = setTextColor(FG_RED)

  /**
   * Makes text print on the terminal in blue.
   */
  def textBlue(): Unit = setTextColor(FG_BLUE)

  /**
   * Makes text print on the terminal in red.
   */
  def textGreen(): Unit = setTextColor(FG_GREEN)

  /**
   * Makes text print on the terminal in red.
   */
  def textMagenta(): Unit = setTextColor(FG_MAGENTA)

  /**
   * Set the terminal title.
   *
   * @param title New terminal title.
   */
  def setTitle(title: String): Unit = {
    writeBytes(AnsiTerminal.SET_TERM_TITLE, title.getBytes, Array(AnsiTerminal.BEL))
  }

  /**
   * Writes a string to the terminal using the current font, color and cursor
   * position settings.
   *
   * @param text the text to write
   */
  def writeString(text: String): Unit = out.write(text.getBytes)

  /**
   * Writes a byte sequence to the terminal using the current font, color and cursor position
   * settings.
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
  private def writeEscapeSequence(bytes: Byte*): Unit = writeBytes(ESC, bytes.toArray)

  /**
   * Utility method for generating control sequences. Takes a collection of byte arrays, which
   * contains the components of a control sequence, concatenates them, and prints them to the
   * terminal.
   *
   * @param stuff the byte arrays that make up the sequence to be sent to the terminal
   */
  private def writeBytes(stuff: Array[Byte]*): Unit = stuff.foreach(out.write)

  /**
   * Sends a carriage return to the terminal.
   */
  def cr(): Unit = writeBytes(CR)

  /**
   * Flushes the underlying stream. This class does not do any buffering of its own, but the
   * underlying OutputStream may.
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