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

abstract class FileEventTransport(path: Path) extends EventTransport {
  // Note: I did an attempt at using com.twitter.io.Writer in order to provide asynchronicity.
  // However, it seems that nothing prevented concurrent writes to the same file.
  // TODO: investigate how to introduce thread-safe asynchronicity.
  private[this] val os = new BufferedOutputStream(new FileOutputStream(path.toFile))

  override final def sendEvent(event: Event): Future[Unit] = synchronized {
    val bytes = serialize(event)
    os.write(bytes, 0, bytes.length)
    Future.Done
  }

  override final def close(deadline: Time): Future[Unit] = {
    os.close()
    Future.Done
  }

  protected def serialize(event: Event): Array[Byte]
}
