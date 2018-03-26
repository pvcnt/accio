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

package fr.cnrs.liris.accio.tools.cli.command

import java.io.{IOException, OutputStream, PrintStream}

import com.twitter.util.Duration
import com.twitter.inject.Logging
import fr.cnrs.liris.accio.tools.cli.event.{Event, EventHandler, EventKind}
import fr.cnrs.liris.common.flags.Flag
import fr.cnrs.liris.common.io.OutErr
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable

private[command] case class TerminalEventHandlerOpts(
  @Flag(
    name = "show-progress-rate",
    help = "Minimum duration between progress messages in the output.")
  // A nice middle ground; snappy but not too spammy in logs.
  showProgressRateLimit: Duration = Duration.fromMilliseconds(30),
  @Flag(
    name = "show-progress",
    help = "Display progress messages.")
  showProgress: Boolean = true,
  @Flag(
    name = "show-task-finish",
    help = "Display progress messages when tasks complete, not just when they start.")
  showTaskFinish: Boolean = false,
  @Flag(
    name = "color",
    help = "Use terminal controls to colorize output.")
  useColor: Boolean = true,
  @Flag(
    name = "curses",
    help = "Use terminal cursor controls to minimize scrolling output")
  useCurses: Boolean = true,
  @Flag(
    name = "quiet",
    help = "Suppress all output, even for errors. Use exit code to determine the outcome.")
  quiet: Boolean = false,
  @Flag(
    name = "progress-in-terminal-title",
    help = "Show the command progress in the terminal title. Useful to see what Accio is doing when having " +
      "multiple terminal tabs.")
  progressInTermTitle: Boolean = false,
  @Flag(
    name = "show-timestamps",
    help = "Include timestamps in messages")
  showTimestamp: Boolean = false)

private[command] class TerminalEventHandler(outErr: OutErr, opts: TerminalEventHandlerOpts)
  extends EventHandler with Logging {

  private[this] val errPrintStream = new PrintStream(outErr.err, true)

  protected[this] val eventMask: Set[EventKind] = {
    if (opts.quiet) {
      // Quiet flag disables all output, independently of other flags.
      Set.empty
    } else {
      val mask = mutable.Set.empty[EventKind]
      mask ++= EventKind.ErrorsWarningsInfosOutput
      if (opts.showProgress) {
        mask += EventKind.Progress
        mask + EventKind.Start
      }
      if (opts.showTaskFinish) {
        mask += EventKind.Finish
      }
      mask.toSet
    }
  }

  override def handle(event: Event): Unit = {
    if (!eventMask.contains(event.kind)) {
      return
    }
    var prefix = ""
    event.kind match {
      case EventKind.Stdout =>
        putOutput(outErr.out, event)
        return
      case EventKind.Stderr =>
        putOutput(outErr.err, event)
        return
      case EventKind.Error | EventKind.Warning => prefix = event.kind + ": "
      case EventKind.Info | EventKind.Progress | EventKind.Start | EventKind.Finish => prefix = "____"
    }
    val buf = new StringBuilder
    buf.append(prefix)
    if (opts.showTimestamp) {
      buf.append(timestamp())
    }
    /*Location location = event.getLocation();
    if (location != null) {
      buf.append(location.print()).append(": ");
    }*/
    buf.append(event.message)
    if (event.kind == EventKind.Finish) {
      buf.append(" DONE")
    }

    // Add a trailing period for ERROR and WARNING messages, which are
    // typically English sentences composed from exception messages.
    if (event.kind == EventKind.Warning || event.kind == EventKind.Error) {
      buf.append('.')
    }

    // Event messages go to stderr; results (e.g. 'accio version') go to stdout.
    errPrintStream.println(buf)
  }

  private def putOutput(out: OutputStream, event: Event): Unit = {
    try {
      out.write(event.bytes)
      out.flush()
    } catch {
      case e: IOException =>
        // This can happen in server mode if the blaze client has exited, or if output is redirected
        // to a file and the disk is full, etc. May be moot in the case of full disk, or useful in
        // the case of real bug in our handling of streams.
        logger.warn("Failed to write event", e)
    }
  }

  /**
   * @return a string representing the current time, eg "04-26 13:47:32.124".
   */
  protected def timestamp(): String = TerminalEventHandler.TimestampFormat.print(System.currentTimeMillis())
}

object TerminalEventHandler {
  private val TimestampFormat = DateTimeFormat.forPattern("(MM-dd HH:mm:ss.SSS) ")
}
