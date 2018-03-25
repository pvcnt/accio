/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.runtime.event

import java.io.OutputStream
import java.util

/**
 * An OutputStream that delegates all writes to an EventHandler.
 */
final class ReporterStream(handler: EventHandler, eventKind: EventKind) extends OutputStream {
  override def close(): Unit = {
    // NOP.
  }

  override def flush(): Unit = {
    // NOP.
  }

  override def write(b: Int): Unit = handler.handle(Event(eventKind, Array(b.toByte)))

  override def write(bytes: Array[Byte]): Unit = write(bytes, 0, bytes.length)

  override def write(bytes: Array[Byte], offset: Int, len: Int): Unit = {
    handler.handle(Event(eventKind, util.Arrays.copyOfRange(bytes, offset, offset + len)))
  }
}