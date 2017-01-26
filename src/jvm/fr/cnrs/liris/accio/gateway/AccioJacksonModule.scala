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

package fr.cnrs.liris.accio.gateway

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftStructCodec, ThriftUnion}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.geo.Distance

import scala.reflect.{ClassTag, classTag}

/**
 * Jackson module providing serializers for Accio Thrift structures.
 */
object AccioJacksonModule extends SimpleModule {
  addSerializer(new ScroogeStructSerializer[Run](Run))
  addSerializer(new ScroogeStructSerializer[RunState](RunState))
  addSerializer(new ScroogeStructSerializer[Package](Package))
  addSerializer(new ScroogeStructSerializer[NodeState](NodeState))
  addSerializer(new ScroogeStructSerializer[Artifact](Artifact))
  addSerializer(new ScroogeStructSerializer[Metric](Metric))
  addSerializer(new ScroogeStructSerializer[ErrorData](ErrorData))
  addSerializer(new ScroogeStructSerializer[Error](Error))
  addSerializer(new ScroogeStructSerializer[RunLog](RunLog))
  addSerializer(new ScroogeStructSerializer[OpResult](OpResult))
  addSerializer(new ScroogeStructSerializer[Workflow](Workflow))
  addSerializer(new ScroogeStructSerializer[NodeDef](NodeDef))
  addSerializer(new ScroogeStructSerializer[Reference](Reference))
  addSerializer(new ScroogeStructSerializer[User](User))
  addSerializer(new ScroogeStructSerializer[DataType](DataType))
  addSerializer(new ScroogeStructSerializer[Artifact](Artifact))
  addSerializer(new ScroogeStructSerializer[ArgDef](ArgDef))

  addSerializer(new ScroogeWrappedStructSerializer[WorkflowId](WorkflowId))
  addSerializer(new ScroogeWrappedStructSerializer[RunId](RunId))
  addSerializer(new ScroogeWrappedStructSerializer[CacheKey](CacheKey))
  addSerializer(new ScroogeWrappedStructSerializer[GraphDef](GraphDef))
  addSerializer(new ScroogeUnionSerializer[InputDef](InputDef))

  addSerializer(new ScroogeValueSerializer)
  addSerializer(new ScroogeEnumSerializer)
  addSerializer(new ScroogeDistanceSerializer)
}

private class ScroogeEnumSerializer extends StdSerializer[ThriftEnum](classOf[ThriftEnum]) {
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

private class ScroogeWrappedStructSerializer[T <: ThriftStruct : ClassTag](codec: ThriftStructCodec[T]) extends StdSerializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def serialize(t: T, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val fieldInfo = codec.metaData.fieldInfos.head
    val rawValue = _handledType.getMethod(fieldInfo.tfield.name).invoke(t)
    rawValue match {
      case None =>
      case Some(v) => jsonGenerator.writeObject(v)
      case _ => jsonGenerator.writeObject(rawValue)
    }
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

private class ScroogeValueSerializer extends StdSerializer[Value](classOf[Value]) {
  override def serialize(t: Value, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeStartObject()
    jsonGenerator.writeObjectField("kind", t.kind)
    jsonGenerator.writeObjectField("payload", Values.decode(t))
    jsonGenerator.writeEndObject()
  }
}

private class ScroogeDistanceSerializer extends StdSerializer[Distance](classOf[Distance]) {
  override def serialize(t: Distance, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeNumber(t.meters)
  }
}