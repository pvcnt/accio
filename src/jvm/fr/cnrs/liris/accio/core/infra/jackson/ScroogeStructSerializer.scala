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

package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct}
import org.apache.thrift.protocol.TSimpleJSONProtocol

final class ScroogeStructSerializer extends StdSerializer[ThriftStruct](classOf[ThriftStruct]) {
  override def serialize(t: ThriftStruct, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val transport = new TArrayByteTransport
    val protocol = new TSimpleJSONProtocol.Factory().getProtocol(transport)
    t.write(protocol)
    val bytes = transport.toByteArray
    jsonGenerator.writeRaw(new String(bytes))
  }
}