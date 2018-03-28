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

package fr.cnrs.liris.common.scrooge

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import com.twitter.util.Codec
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol}

abstract class ThriftCodec[T <: ThriftStruct](codec: ThriftStructCodec[T])
  extends Codec[T, Array[Byte]] with ScroogeSerializer {

  override def encode(obj: T): Array[Byte] = toBytes(obj)

  override def decode(bytes: Array[Byte]): T = fromBytes(bytes, codec)
}

final class BinaryThriftCodec[T <: ThriftStruct](codec: ThriftStructCodec[T]) extends ThriftCodec(codec) {
  override protected val protocolFactory = new TBinaryProtocol.Factory
}

final class CompactThriftCodec[T <: ThriftStruct](codec: ThriftStructCodec[T]) extends ThriftCodec(codec) {
  override protected val protocolFactory = new TCompactProtocol.Factory
}