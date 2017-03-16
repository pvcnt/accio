// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package fr.cnrs.liris.accio.client.runtime

import java.io.IOException

import com.google.common.base.Splitter
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.client.event.{Event, EventKind}
import fr.cnrs.liris.common.io._

/**
 * An event handler for ANSI terminals which uses control characters to provide eye-candy, reduce scrolling, and
 * generally improve usability for users running directly from the shell.
 *
 * <p>This event handler differs from a normal terminal because it only adds control characters to stderr, not stdout.
 * All blaze status feedback is sent to stderr, so adding control characters just to that stream gives the benefits
 * described above without modifying the normal output stream. For commands that don't generate stdout output this
 * doesn't matter, but for others, inserting these control characters in stdout invalidated their output.
 *
 * <p> The underlying streams may be either line-buffered or unbuffered. Normally each event will write out a sequence
 * of output to a single stream, and will end with a newline, which ensures a flush. But care is required when
 * outputting incomplete lines, or when mixing output between the two different streams (stdout and stderr): it may be
 * necessary to explicitly flush the output in those cases. However, we also don't want to flush too often; that can
 * lead to a choppy UI experience.
 */
final class FancyTerminalEventHandler(outErr: OutErr, opts: CommandEventHandlerOpts)
  extends CommandEventHandler(outErr, opts) with LazyLogging {

  import FancyTerminalEventHandler._

  private[this] val terminal = new AnsiTerminal(outErr.err)
  private[this] val terminalWidth = 80
  private[this] var terminalClosed = false
  private[this] var previousLineErasable = false
  private[this] var numLinesPreviousErasable = 0

  override def handle(event: Event): Unit = {
    if (terminalClosed) {
      return
    }
    if (!eventMask.contains(event.kind)) {
      return
    }
    try {
      var previousLineErased = false
      if (previousLineErasable) {
        previousLineErased = maybeOverwritePreviousMessage()
      }
      event.kind match {
        case EventKind.Progress | EventKind.Start =>
          event.message match {
            case ProgressPattern(percentage, rest) => progress(percentage, rest)
            case message => progress("INFO: ", message)
          }
        case EventKind.Finish =>
          event.message match {
            case ProgressPattern(percentage, rest) => progress(percentage, rest + " DONE")
            case message => progress("INFO: ", message + " DONE")
          }
        case EventKind.Info => info(event)
        case EventKind.Error =>
          // For errors, scroll the message, so it appears above the status
          // line, and highlight the word "ERROR" or "FAIL" in boldface red.
          errorOrFail(event)
        case EventKind.Warning =>
          // For warnings, highlight the word "Warning" in boldface magenta,
          // and scroll it.
          warning(event)
        case EventKind.Stdout =>
          if (previousLineErased) {
            terminal.flush()
          }
          previousLineErasable = false
          // We don't need to flush stdout here, because
          // super.handle(event) will take care of that.
          super.handle(event)
        case EventKind.Stderr => putOutput(event)
        case _ => // Ignore all other event types.
      }
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

  /**
   * Displays a progress message that may be erased by subsequent messages.
   *
   * @param  prefix a short string such as "[99%] " or "INFO: ", which will be highlighted
   * @param  rest   the remainder of the message; may be multiple lines
   */
  private def progress(prefix: String, rest: String) = {
    previousLineErasable = true
    if (opts.progressInTermTitle) {
      val newlinePos = rest.indexOf('\n')
      if (newlinePos == -1) {
        terminal.setTitle(prefix + rest)
      } else {
        terminal.setTitle(prefix + rest.substring(0, newlinePos))
      }
    }

    val countingWriter = new LineCountingAnsiTerminalWriter(terminal)
    var terminalWriter: AnsiTerminalWriter = countingWriter

    if (opts.useCurses) {
      terminalWriter = new LineWrappingAnsiTerminalWriter(terminalWriter, terminalWidth - 1)
    }
    if (opts.useColor) {
      terminalWriter.okStatus()
    }
    terminalWriter.append(prefix)
    terminalWriter.normal()
    if (opts.showTimestamp) {
      val timestamp = timestamp()
      terminalWriter.append(timestamp)
    }
    val lines = LineBreakSplitter.split(rest).iterator
    val firstLine = lines.next()
    terminalWriter.append(firstLine)
    terminalWriter.newline()
    while (lines.hasNext) {
      terminalWriter.append(lines.next)
      terminalWriter.newline()
    }
    numLinesPreviousErasable = countingWriter.writtenLines
  }

  /**
   * Send the terminal controls that will put the cursor on the beginning
   * of the same line if cursor control is on, or the next line if not.
   *
   * @return True if it did any output; if so, caller is responsible for
   *         flushing the terminal if needed.
   */
  private def maybeOverwritePreviousMessage() = {
    if (opts.useCurses && numLinesPreviousErasable != 0) {
      (0 until numLinesPreviousErasable).foreach { _ =>
        terminal.cr()
        terminal.cursorUp(1)
        terminal.clearLine();
      }
      true
    } else {
      false
    }
  }

  private def errorOrFail(event: Event) = {
    previousLineErasable = false
    if (opts.useColor) {
      terminal.textRed()
      terminal.textBold()
    }
    terminal.writeString(event.kind + ": ")
    if (opts.useColor) {
      terminal.resetTerminal()
    }
    writeTimestampAndLocation(event)
    writeStringWithPotentialPeriod(event.message)
    crlf()
  }

  private def warning(warning: Event) = {
    previousLineErasable = false
    if (opts.useColor) {
      terminal.textMagenta()
    }
    terminal.writeString("WARNING: ")
    terminal.resetTerminal()
    writeTimestampAndLocation(warning)
    writeStringWithPotentialPeriod(warning.message)
    crlf()
  }

  private def info(event: Event) = {
    previousLineErasable = false
    if (opts.useColor) {
      terminal.textGreen()
    }
    terminal.writeString(event.kind + ": ")
    terminal.resetTerminal()
    writeTimestampAndLocation(event)
    terminal.writeString(event.message)
    // No period; info messages may end with a URL.
    crlf()
  }

  /**
   * Writes the given String to the terminal. This method also writes a trailing period if the
   * message doesn't end with a punctuation character.
   */
  private def writeStringWithPotentialPeriod(message: String) = {
    terminal.writeString(message)
    if (message.nonEmpty) {
      val lastChar = message.last
      if (!Punctuation.contains(lastChar)) {
        terminal.writeString(".")
      }
    }
  }

  /**
   * Handle STDERR events.
   *
   * @param event Event
   */
  private def putOutput(event: Event) = {
    previousLineErasable = false
    terminal.writeBytes(event.bytes)
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
  private def crlf() = {
    terminal.cr()
    terminal.writeString("\n")
  }

  private def writeTimestampAndLocation(event: Event) = {
    if (opts.showTimestamp) {
      terminal.writeString(timestamp())
    }
    /*if (event.getLocation() != null) {
      terminal.writeString(event.getLocation() + ": ");
    }*/
  }

  def resetTerminal(): Unit = {
    try {
      terminal.resetTerminal()
    } catch {
      case e: IOException => logger.warn("IO Error writing to user terminal", e)
    }
  }
}

object FancyTerminalEventHandler {
  // Match strings that look like they start with progress info:
  //   [42%] Compiling base/base.cc
  //   [1,442 / 23,476] Compiling base/base.cc
  private val ProgressPattern = "^\\([(?:(?:\\d\\d?\\d?%)|(?:[\\d+,]+ / [\\d,]+))\\] )(.*)".r
  private val LineBreakSplitter = Splitter.on('\n')
  private val Punctuation = Set(',', '.', ':', '?', '!', ';')
}