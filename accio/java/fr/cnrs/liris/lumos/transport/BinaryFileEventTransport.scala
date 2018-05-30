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

import java.nio.file.Path

import fr.cnrs.liris.lumos.domain.Event
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

/**
 * An event transport that writes events into a file, serialized in Thrift binary format.
 *
 * @param file File where to write the events.
 */
final class BinaryFileEventTransport(file: Path) extends FileEventTransport(file) {
  override def name = "BinaryFile"

  override protected def serialize(event: Event): Array[Byte] = {
    BinaryScroogeSerializer.toBytes(ThriftAdapter.toThrift(event))
  }
}
