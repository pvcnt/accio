/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

final class ScroogeEnumDeserializer[T <: ThriftEnum : ClassTag](codec: Any) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  require(codec.getClass.getName == _valueClass.getName + "$")

  override def deserialize(jp: JsonParser, ctx: DeserializationContext): T = {
    val id = jp.getValueAsInt
    val method = codec.getClass.getMethod("get", classOf[Int])
    method.invoke(codec, id: java.lang.Integer) match {
      case Some(o: T) => o
      case None => throw new RuntimeException(s"Unable to parse $id into ${_valueClass.getName}")
    }
  }
}