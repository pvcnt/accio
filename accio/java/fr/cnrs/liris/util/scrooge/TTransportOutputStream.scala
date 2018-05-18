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

package fr.cnrs.liris.util.scrooge

import java.io.{ByteArrayOutputStream, IOException}

import org.apache.thrift.transport.TTransport

import scala.util.control.NonFatal

/**
 * A byte array output stream that forwards all data to a TTransport when it is flushed or closed.
 */
private class TTransportOutputStream(transport: TTransport) extends ByteArrayOutputStream {
  override def close(): Unit = {
    // This isn't necessary, but a good idea to close the transport
    flush()
    super.close()
    transport.close()
  }

  override def flush(): Unit = {
    try {
      super.flush()
      val bytes = toByteArray
      transport.write(bytes)
      transport.flush()

    } catch {
      case NonFatal(e) => throw new IOException(e)
    }
    // Clears the internal memory buffer, since we've already written it out.
    super.reset()
  }
}