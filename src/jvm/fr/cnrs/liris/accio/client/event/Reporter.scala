package fr.cnrs.liris.accio.client.event

import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.io.OutErr

final class Reporter(handlers: Seq[EventHandler]) extends EventHandler with ExceptionListener with LazyLogging {
  /**
   * An OutErr that sends all of its output to this Reporter.
   * Each write will (when flushed) get mapped to an EventKind.STDOUT or EventKind.STDERR event.
   */
  val outErr = new OutErr(
    new ReporterStream(this, EventKind.Stdout),
    new ReporterStream(this, EventKind.Stderr))

  override def handle(event: Event): Unit = {
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
