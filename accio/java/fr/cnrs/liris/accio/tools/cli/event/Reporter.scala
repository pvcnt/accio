

package fr.cnrs.liris.accio.tools.cli.event

import fr.cnrs.liris.accio.tools.cli.terminal.OutErr

final class Reporter(handlers: Seq[EventHandler], outputFilter: OutputFilter = OutputFilter.Everything)
  extends EventHandler with ExceptionListener {

  /**
   * An OutErr that sends all of its output to this Reporter.
   * Each write will (when flushed) get mapped to an EventKind.STDOUT or EventKind.STDERR event.
   */
  val outErr = new OutErr(
    new ReporterStream(this, EventKind.Stdout),
    new ReporterStream(this, EventKind.Stderr))

  override def handle(event: Event): Unit = {
    if (event.kind != EventKind.Error && event.tag.isDefined && !outputFilter.showOutput(event.tag.get)) {
      return
    }
    handlers.foreach(_.handle(event))
  }

  override def error(message: String, e: Throwable): Unit = {
    handle(Event.error(s"$message. ${formatMessage(e)}"))
    //logger.error(message, e)
  }

  private def formatMessage(e: Throwable) =
    e match {
      case _: IllegalArgumentException => e.getMessage.stripPrefix("requirement failed: ")
      case _ => e.getMessage
    }
}
