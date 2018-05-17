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

import java.nio.ByteBuffer

import org.apache.thrift.protocol._

trait WriteOnlyTProtocol extends TProtocol {
  override def readStructBegin(): TStruct = throw new UnsupportedOperationException

  override def readStructEnd(): Unit = throw new UnsupportedOperationException

  override def readSetEnd(): Unit = throw new UnsupportedOperationException

  override def readI64(): Long = throw new UnsupportedOperationException

  override def readListBegin(): TList = throw new UnsupportedOperationException

  override def readSetBegin(): TSet = throw new UnsupportedOperationException

  override def readListEnd(): Unit = throw new UnsupportedOperationException

  override def readBinary(): ByteBuffer = throw new UnsupportedOperationException

  override def readFieldBegin(): TField = throw new UnsupportedOperationException

  override def readMapBegin(): TMap = throw new UnsupportedOperationException

  override def readMessageEnd(): Unit = throw new UnsupportedOperationException

  override def readI16(): Short = throw new UnsupportedOperationException

  override def readDouble(): Double = throw new UnsupportedOperationException

  override def readMessageBegin(): TMessage = throw new UnsupportedOperationException

  override def readBool(): Boolean = throw new UnsupportedOperationException

  override def readString(): String = throw new UnsupportedOperationException

  override def readFieldEnd(): Unit = throw new UnsupportedOperationException

  override def readMapEnd(): Unit = throw new UnsupportedOperationException

  override def readI32(): Int = throw new UnsupportedOperationException

  override def readByte(): Byte = throw new UnsupportedOperationException
}
