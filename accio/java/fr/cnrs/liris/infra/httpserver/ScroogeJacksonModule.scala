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

package fr.cnrs.liris.infra.httpserver

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.google.common.base.CaseFormat
import com.twitter.scrooge._

/**
 * Jackson module providing serializers for Scrooge structures.
 */
object ScroogeJacksonModule extends SimpleModule {
  addSerializer(ScroogeStructSerializer)
  addSerializer(ScroogeUnionSerializer)
  addSerializer(ScroogeEnumSerializer)
}

private object ScroogeEnumSerializer extends StdSerializer[ThriftEnum](classOf[ThriftEnum]) {
  override def serialize(enum: ThriftEnum, jsonGen: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGen.writeString(enum.name)
  }
}

private object ScroogeStructSerializer extends StdSerializer[ThriftStruct](classOf[ThriftStruct]) {
  override def serialize(struct: ThriftStruct, jsonGen: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val codec = struct.asInstanceOf[HasThriftStructCodec3[_]]._codec
    val product = struct.asInstanceOf[Product]
    jsonGen.writeStartObject()
    codec.metaData.fieldInfos.zipWithIndex.foreach { case (fieldInfo, idx) =>
      val fieldName = Inflector.lowerCamel(fieldInfo.tfield.name)
      val rawValue = product.productElement(idx)
      rawValue match {
        case None => // Do not serialize None's.
        case Some(v) => jsonGen.writeObjectField(fieldName, v)
        case _ => jsonGen.writeObjectField(fieldName, rawValue)
      }
    }
    jsonGen.writeEndObject()
  }
}

private object ScroogeUnionSerializer extends StdSerializer[ThriftUnion](classOf[ThriftUnion]) {
  override def serialize(union: ThriftUnion, jsonGen: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val codec = union.asInstanceOf[HasThriftStructCodec3[_]]._codec
    val struct = union.asInstanceOf[ThriftUnion with ThriftStruct]
    val maybeFieldInfo = codec.metaData.unionFields.find(union.getClass == _.fieldClassTag.runtimeClass)
    maybeFieldInfo.foreach { fieldInfo =>
      jsonGen.writeStartObject()
      val fieldName = Inflector.lowerCamel(fieldInfo.structFieldInfo.tfield.name)
      jsonGen.writeObjectField(fieldName, fieldInfo.fieldValue(struct))
      jsonGen.writeEndObject()
    }
  }
}

private object Inflector {
  def lowerCamel(str: String): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str)
}