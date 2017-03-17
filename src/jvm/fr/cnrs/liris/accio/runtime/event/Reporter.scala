/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.runtime.event

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
