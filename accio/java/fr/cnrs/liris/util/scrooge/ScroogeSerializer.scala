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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}

import com.twitter.io.Buf
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import com.twitter.util.Base64StringEncoder
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TProtocolFactory}
import org.apache.thrift.transport.TIOStreamTransport

trait ScroogeSerializer {
  def fromString[T <: ThriftStruct](str: String, codec: ThriftStructCodec[T]): T =
    fromBytes(Base64StringEncoder.decode(str), codec)

  def fromBytes[T <: ThriftStruct](bytes: Array[Byte], codec: ThriftStructCodec[T]): T =
    read(new ByteArrayInputStream(bytes), codec)

  def read[T <: ThriftStruct](is: InputStream, codec: ThriftStructCodec[T]): T = {
    val protocol = protocolFactory.getProtocol(new TIOStreamTransport(is))
    codec.decode(protocol)
  }

  def toString[T <: ThriftStruct](obj: T): String = Base64StringEncoder.encode(toBytes(obj))

  def toBytes[T <: ThriftStruct](obj: T): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    write(obj, baos)
    baos.toByteArray
  }

  def toBuf[T <: ThriftStruct](obj: T): Buf = Buf.ByteArray.Owned(toBytes(obj))

  def write[T <: ThriftStruct](obj: T, os: OutputStream): Unit = {
    val protocol = protocolFactory.getProtocol(new TIOStreamTransport(os))
    obj.write(protocol)
  }

  protected def protocolFactory: TProtocolFactory
}

object BinaryScroogeSerializer extends ScroogeSerializer {
  override protected val protocolFactory = new TBinaryProtocol.Factory
}

object CompactScroogeSerializer extends ScroogeSerializer {
  override protected val protocolFactory = new TCompactProtocol.Factory
}

object TextScroogeSerializer extends ScroogeSerializer {
  override protected val protocolFactory = new TTextProtocol.Factory
}