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

package fr.cnrs.liris.accio.tools.gateway

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftStructCodec, ThriftUnion}
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.util.geo.Distance

import scala.reflect.{ClassTag, classTag}

/**
 * Jackson module providing serializers for Accio Thrift structures.
 */
object AccioJacksonModule extends SimpleModule {
  addSerializer(new ScroogeStructSerializer[Job](Job))
  addSerializer(new ScroogeStructSerializer[JobStatus](JobStatus))
  addSerializer(new ScroogeStructSerializer[Task](Task))
  addSerializer(new ScroogeStructSerializer[NamedValue](NamedValue))
  addSerializer(new ScroogeStructSerializer[NamedChannel](NamedChannel))
  addSerializer(new ScroogeStructSerializer[Metric](Metric))
  addSerializer(new ScroogeStructSerializer[Reference](Reference))
  addSerializer(new ScroogeStructSerializer[Export](Export))
  addSerializer(new ScroogeStructSerializer[User](User))
  addSerializer(new ScroogeStructSerializer[Attribute](Attribute))

  addSerializer(new ScroogeUnionSerializer[Channel](Channel))
  addSerializer(new ScroogeUnionSerializer[DataType](DataType))

  addSerializer(ScroogeValueSerializer)
  addSerializer(ScroogeEnumSerializer)
  addSerializer(ScroogeDistanceSerializer)
}

private object ScroogeEnumSerializer extends StdSerializer[ThriftEnum](classOf[ThriftEnum]) {
  override def serialize(t: ThriftEnum, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeString(t.name.toLowerCase)
  }
}

private class ScroogeStructSerializer[T <: ThriftStruct with Product : ClassTag](codec: ThriftStructCodec[T]) extends StdSerializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def serialize(t: T, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeStartObject()
    codec.metaData.fieldInfos.zipWithIndex.foreach { case (fieldInfo, idx) =>
      val rawValue = t.productElement(idx)
      rawValue match {
        case None =>
        case Some(v) => jsonGenerator.writeObjectField(fieldInfo.tfield.name, v)
        case _ => jsonGenerator.writeObjectField(fieldInfo.tfield.name, rawValue)
      }
    }
    jsonGenerator.writeEndObject()
  }
}

private class ScroogeUnionSerializer[T <: ThriftStruct with ThriftUnion : ClassTag](codec: ThriftStructCodec[T]) extends StdSerializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def serialize(t: T, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val maybeFieldInfo = codec.metaData.unionFields.find(t.getClass == _.fieldClassTag.runtimeClass)
    maybeFieldInfo.foreach { fieldInfo =>
      jsonGenerator.writeStartObject()
      val rawValue = fieldInfo.fieldValue(t)
      jsonGenerator.writeObjectField(fieldInfo.structFieldInfo.tfield.name, rawValue)
      jsonGenerator.writeEndObject()
    }
  }
}

private object ScroogeValueSerializer extends StdSerializer[Value](classOf[Value]) {

  private case class JsonValue(dataType: DataType, payload: Any)

  override def serialize(t: Value, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeObject(JsonValue(t.dataType, Values.decode(t)))
  }
}

private object ScroogeDistanceSerializer extends StdSerializer[Distance](classOf[Distance]) {
  override def serialize(t: Distance, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeNumber(t.meters)
  }
}