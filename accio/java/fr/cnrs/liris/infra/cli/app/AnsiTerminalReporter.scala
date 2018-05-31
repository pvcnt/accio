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

package fr.cnrs.liris.infra.cli.app

import java.io.IOException

import com.twitter.util.logging.Logging
import fr.cnrs.liris.infra.cli.io.{AnsiTerminal, OutErr}
import org.joda.time.format.DateTimeFormat

final class AnsiTerminalReporter(val outErr: OutErr, opts: AnsiTerminalReporter.Options)
  extends Reporter with Logging {

  import AnsiTerminalReporter._

  private[this] val terminal = new AnsiTerminal(outErr.err)
  private[this] var terminalClosed = false

  override def warn(message: String): Unit = handle {
    if (opts.useColor) {
      terminal.textMagenta()
    }
    terminal.writeString("WARNING: ")
    terminal.resetTerminal()
    startLine()
    writeStringWithPotentialPeriod(message)
    crlf()
  }

  override def error(message: String): Unit = handle {
    if (opts.useColor) {
      terminal.textRed()
      terminal.textBold()
    }
    terminal.writeString("ERROR: ")
    if (opts.useColor) {
      terminal.resetTerminal()
    }
    startLine()
    writeStringWithPotentialPeriod(message)
    crlf()
  }

  override def info(message: String): Unit = handle {
    if (opts.useColor) {
      terminal.textGreen()
    }
    terminal.writeString("INFO: ")
    terminal.resetTerminal()
    startLine()
    terminal.writeString(message)
    // No period; info messages may end with a URL.
    crlf()
  }

  override def error(message: String, e: Throwable): Unit = {
    val details = e match {
      case _: IllegalArgumentException => e.getMessage.stripPrefix("requirement failed: ")
      case _ => e.getMessage
    }
    error(s"$message. $details")
  }

  def resetTerminal(): Unit = {
    try {
      terminal.resetTerminal()
    } catch {
      case e: IOException => logger.warn("IO Error writing to user terminal", e)
    }
  }

  /**
   * Writes the given String to the terminal. This method also writes a trailing period if the
   * message doesn't end with a punctuation character.
   */
  private def writeStringWithPotentialPeriod(message: String): Unit = {
    terminal.writeString(message)
    if (message.nonEmpty) {
      val lastChar = message.last
      if (!Punctuation.contains(lastChar)) {
        terminal.writeString(".")
      }
    }
  }

  private def startLine(): Unit = {
    if (opts.showTimestamp) {
      terminal.writeString(TimestampFormat.print(System.currentTimeMillis()))
    }
  }

  /**
   * Add a carriage return, shifting to the next line on the terminal, while
   * guaranteeing that the terminal control codes don't cause any strange
   * effects.  Without the CR before the "\n", the "\n" can cause a line-break
   * moving text to the next line, where the new message will be generated.
   * Emitting a "CR" before means that the actual terminal controls generated
   * here are CR+CR+LF; the double-CR resets the terminal line state, which
   * prevents the potentially ugly formatting issue.
   */
  private def crlf(): Unit = {
    terminal.cr()
    terminal.writeString("\n")
  }

  private def handle[T](fn: => T): Unit = {
    if (!terminalClosed) {
      try {
        fn
      } catch {
        case e: IOException =>
          // The terminal shouldn't have IO errors, unless the shell is killed, which
          // should also kill the client. So this isn't something that should
          // occur here; it will show up in the client/server interface as a broken
          // pipe.
          logger.warn("Terminal was closed during execution", e)
          terminalClosed = true
      }
    }
  }
}

object AnsiTerminalReporter {

  case class Options(useColor: Boolean, showTimestamp: Boolean)

  private val Punctuation = Set(',', '.', ':', '?', '!', ';')
  private val TimestampFormat = DateTimeFormat.forPattern("(MM-dd HH:mm:ss.SSS) ")
}