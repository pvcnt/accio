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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.twitter.scrooge.ThriftEnum

import scala.reflect._

/**
 * Jackson deserializer handling Thrift/Scrooge enums. Because enum "codecs" do not have a root type (in fact they do
 * not inherit anything besides Any), we cannot perform any type check here, apart from the class name. It is up to
 * the developer to provide a consistent instantiation of this class, including defining the class tag.
 *
 * Implementation is quite ugly, but should be stable enough for production use.
 *
 * @param codec Enum "codec".
 * @tparam T Type of enum being deserialized.
 */
final class ScroogeEnumDeserializer[T <: ThriftEnum : ClassTag](codec: Any) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  // Yes, this is it. The consistency check...
  require(codec.getClass.getName == _valueClass.getName + "$")

  override def deserialize(jp: JsonParser, ctx: DeserializationContext): T = {
    val id = jp.getValueAsInt
    val method = codec.getClass.getMethod("get", classOf[Int])
    method.invoke(codec, id: java.lang.Integer) match {
      case Some(o: T) => o
      case None => throw new ThriftProtocolException(s"Unable to parse enum value $id into ${_valueClass.getName}")
    }
  }
}