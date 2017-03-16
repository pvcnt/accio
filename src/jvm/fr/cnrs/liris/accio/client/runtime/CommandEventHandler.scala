package fr.cnrs.liris.accio.client.runtime

import java.io.{IOException, OutputStream, PrintStream}

import com.twitter.util.Duration
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.client.event.{Event, EventHandler, EventKind}
import fr.cnrs.liris.common.flags.Flag
import fr.cnrs.liris.common.io.OutErr
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable

private[runtime] case class CommandEventHandlerOpts(
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

class CommandEventHandler(outErr: OutErr, opts: CommandEventHandlerOpts) extends EventHandler with LazyLogging {
  private[this] val errPrintStream = new PrintStream(outErr.err, true)
  protected val eventMask: Set[EventKind] = {
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
      case EventKind.Stdout => putOutput(outErr.out, event)
      case EventKind.Stderr => putOutput(outErr.err, event)
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

  private def putOutput(out: OutputStream, event: Event) = {
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
  protected def timestamp(): String = CommandEventHandler.TimestampFormat.print(System.currentTimeMillis())
}

object CommandEventHandler {
  private val TimestampFormat = DateTimeFormat.forPattern("(MM-dd HH:mm:ss.SSS) ")
}