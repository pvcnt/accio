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

package fr.cnrs.liris.lumos.transport

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.Path

import com.twitter.util.{Future, Time}
import fr.cnrs.liris.lumos.domain.Event

/**
 * Base class for event transports that write events into files.
 *
 * @param file File where to write the events.
 */
abstract class FileEventTransport(file: Path) extends EventTransport {
  // TODO: make writes asynchronous. The difficult point is to avoid having concurrent writes
  // in the same file, which would mess things up.
  private[this] val os = new BufferedOutputStream(new FileOutputStream(file.toFile))

  override final def sendEvent(event: Event): Unit = synchronized {
    val bytes = serialize(event)
    os.write(bytes, 0, bytes.length)
  }

  override final def close(deadline: Time): Future[Unit] = {
    os.close()
    Future.Done
  }

  /**
   * Serialize an event into bytes. Those bytes will be written as-is into the target file.
   *
   * @param event Events to serialize.
   */
  protected def serialize(event: Event): Array[Byte]
}
