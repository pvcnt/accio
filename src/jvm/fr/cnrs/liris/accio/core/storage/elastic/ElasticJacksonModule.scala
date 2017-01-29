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

package fr.cnrs.liris.accio.core.storage.elastic

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonProcessingException}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, SerializerProvider}
import com.google.common.annotations.VisibleForTesting
import com.twitter.scrooge._
import fr.cnrs.liris.accio.core.domain._
import org.apache.thrift.protocol.{TSimpleJSONProtocol, TType}

import scala.collection.JavaConverters._
import scala.reflect._


/**
 * Jackson module providing serializers and deserializers for Accio Thrift structures. This is an internal format
 * that is nicer than Thrift's JSON protocol but still not ready to be consumed by end users.
 */
object ElasticJacksonModule extends SimpleModule {
  // Run-related deserializers.
  addDeserializer(classOf[Run], new ScroogeStructDeserializer[Run](Run))
  addDeserializer(classOf[RunId], new ScroogeStructDeserializer[RunId](RunId))
  addDeserializer(classOf[RunState], new ScroogeStructDeserializer[RunState](RunState))
  addDeserializer(classOf[Package], new ScroogeStructDeserializer[Package](Package))
  addDeserializer(classOf[NodeState], new ScroogeStructDeserializer[NodeState](NodeState))
  addDeserializer(classOf[Artifact], new ScroogeStructDeserializer[Artifact](Artifact))
  addDeserializer(classOf[Metric], new ScroogeStructDeserializer[Metric](Metric))
  addDeserializer(classOf[ErrorData], new ScroogeStructDeserializer[ErrorData](ErrorData))
  addDeserializer(classOf[Error], new ScroogeStructDeserializer[Error](Error))
  addDeserializer(classOf[RunLog], new ScroogeStructDeserializer[RunLog](RunLog))
  addDeserializer(classOf[OpResult], new ScroogeStructDeserializer[OpResult](OpResult))
  addDeserializer(classOf[CacheKey], new ScroogeStructDeserializer[CacheKey](CacheKey))
  addDeserializer(classOf[NodeStatus], new ScroogeEnumDeserializer[NodeStatus](NodeStatus))
  addDeserializer(classOf[RunStatus], new ScroogeEnumDeserializer[RunStatus](RunStatus))

  // Workflow-related deserializers.
  addDeserializer(classOf[WorkflowId], new ScroogeStructDeserializer[WorkflowId](WorkflowId))
  addDeserializer(classOf[Workflow], new ScroogeStructDeserializer[Workflow](Workflow))
  addDeserializer(classOf[GraphDef], new ScroogeStructDeserializer[GraphDef](GraphDef))
  addDeserializer(classOf[NodeDef], new ScroogeStructDeserializer[NodeDef](NodeDef))
  addDeserializer(classOf[ArgDef], new ScroogeStructDeserializer[ArgDef](ArgDef))
  addDeserializer(classOf[InputDef], new ScroogeUnionDeserializer[InputDef](InputDef))
  addDeserializer(classOf[Reference], new ScroogeStructDeserializer[Reference](Reference))

  // Misc deserializers.
  addDeserializer(classOf[Value], new ScroogeStructDeserializer[Value](Value))
  addDeserializer(classOf[User], new ScroogeStructDeserializer[User](User))
  addDeserializer(classOf[Value], new ScroogeStructDeserializer[Value](Value))
  addDeserializer(classOf[DataType], new ScroogeStructDeserializer[DataType](DataType))
  addDeserializer(classOf[AtomicType], new ScroogeEnumDeserializer[AtomicType](AtomicType))

  // All serializers.
  addSerializer(new ScroogeStructSerializer)
  addSerializer(new ScroogeEnumSerializer)
}

final class ThriftProtocolException(message: String) extends JsonProcessingException(message)

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
@VisibleForTesting
private[elastic] class ScroogeEnumDeserializer[T <: ThriftEnum : ClassTag](codec: Any) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
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

@VisibleForTesting
private[elastic] class ScroogeEnumSerializer extends StdSerializer[ThriftEnum](classOf[ThriftEnum]) {
  override def serialize(t: ThriftEnum, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeString(t.name)
  }
}

@VisibleForTesting
private[elastic] final class ScroogeStructDeserializer[T <: ThriftStruct : ClassTag](codec: ThriftStructCodec[T]) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def deserialize(jp: JsonParser, ctx: DeserializationContext): T = {
    val tree = jp.getCodec.readTree[JsonNode](jp)
    val fields = codec.metaData.fieldInfos.map { fieldInfo =>
      if (tree.has(fieldInfo.tfield.name)) {
        val node = tree.get(fieldInfo.tfield.name)
        parseValue(node, fieldInfo, jp, ctx)
      } else {
        defaultValue(fieldInfo)
      }
    }
    val ctorArgs = fields.map(_.asInstanceOf[AnyRef])
    val method = codec.getClass.getMethods.filter(_.getName == "apply").head
    method.invoke(codec, ctorArgs: _*).asInstanceOf[T]
  }

  private def parseKey(str: String, clazz: Class[_]) = {
    clazz match {
      case _ if clazz == classOf[Boolean] => parseBoolean(str)
      case _ if clazz == classOf[Byte] => str.toByte
      case _ if clazz == classOf[Short] => str.toShort
      case _ if clazz == classOf[Int] => str.toInt
      case _ if clazz == classOf[Long] => str.toLong
      case _ if clazz == classOf[String] => str
      case _ if clazz == classOf[Double] => str.toDouble
      case unsupported => throw new ThriftProtocolException(s"Unsupported key class: $unsupported")
    }
  }

  private def defaultValue(fieldInfo: ThriftStructFieldInfo) =
    fieldInfo.defaultValue match {
      case Some(defaultValue) => if (fieldInfo.isOptional) Some(defaultValue) else defaultValue
      case None =>
        if (fieldInfo.isOptional) {
          None
        } else if (fieldInfo.tfield.`type` == TType.SET) {
          collection.Set.empty
        } else if (fieldInfo.tfield.`type` == TType.LIST) {
          collection.Seq.empty
        } else if (fieldInfo.tfield.`type` == TType.MAP) {
          collection.Map.empty
        } else {
          throw new ThriftProtocolException(s"No value nor default value for field ${fieldInfo.tfield.name}")
        }
    }

  private def parseValue(node: JsonNode, fieldInfo: ThriftStructFieldInfo, jp: JsonParser, ctx: DeserializationContext) = {
    val value = fieldInfo.tfield.`type` match {
      case TType.BOOL => parseBoolean(node.asText)
      case TType.BYTE => node.asInt.toByte
      case TType.DOUBLE => node.asDouble
      case TType.ENUM => jp.getCodec.treeToValue(node, fieldInfo.manifest.runtimeClass)
      case TType.I16 => node.asInt.toShort
      case TType.I32 => node.asInt
      case TType.I64 => node.asLong
      case TType.STRING => node.asText
      case TType.STRUCT => jp.getCodec.treeToValue(node, fieldInfo.manifest.runtimeClass)
      case TType.SET =>
        val valueClass = fieldInfo.valueManifest.get.runtimeClass
        val items = node.elements.asScala.map { childNode =>
          jp.getCodec.treeToValue(childNode, valueClass)
        }.toSeq
        collection.Set(items: _*)
      case TType.LIST =>
        val valueClass = fieldInfo.valueManifest.get.runtimeClass
        val items = node.elements.asScala.map { childNode =>
          jp.getCodec.treeToValue(childNode, valueClass)
        }.toSeq
        collection.Seq(items: _*)
      case TType.MAP =>
        val keyClass = fieldInfo.keyManifest.get.runtimeClass
        val valueClass = fieldInfo.valueManifest.get.runtimeClass
        val items = node.fields.asScala.map { entry =>
          val key = parseKey(entry.getKey, keyClass)
          val value = jp.getCodec.treeToValue(entry.getValue, valueClass)
          key -> value
        }.toSeq
        collection.Map(items: _*)
      case unknown => throw new ThriftProtocolException(s"Unknown type: $unknown")
    }
    if (fieldInfo.isOptional) Some(value) else value
  }

  private def parseBoolean(str: String) = str match {
    case "1" => true
    case "0" => false
    case _ => str.toBoolean
  }
}

@VisibleForTesting
private[elastic] class ScroogeStructSerializer extends StdSerializer[ThriftStruct](classOf[ThriftStruct]) {
  override def serialize(t: ThriftStruct, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    val transport = new TArrayByteTransport
    val protocol = new TSimpleJSONProtocol.Factory().getProtocol(transport)
    t.write(protocol)
    val bytes = transport.toByteArray
    jsonGenerator.writeRaw(new String(bytes))
  }
}

@VisibleForTesting
private[elastic] class ScroogeUnionDeserializer[T <: ThriftStruct with ThriftUnion : ClassTag](codec: ThriftStructCodec[T]) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def deserialize(jp: JsonParser, ctx: DeserializationContext): T = {
    val tree = jp.getCodec.readTree[JsonNode](jp)
    val maybeField = codec.metaData.unionFields.find(fieldInfo => tree.has(fieldInfo.structFieldInfo.tfield.name))
    maybeField match {
      case None => throw new ThriftProtocolException(s"Cannot deserialize union from ${tree.fields.asScala.map(_.getKey).mkString(", ")}")
      case Some(fieldInfo) =>
        val ctorArg = jp.getCodec.treeToValue(tree.get(fieldInfo.structFieldInfo.tfield.name), fieldInfo.structFieldInfo.manifest.runtimeClass)
        val ctor = fieldInfo.fieldClassTag.runtimeClass.getConstructors.head
        ctor.newInstance(ctorArg.asInstanceOf[AnyRef]).asInstanceOf[T]
    }
  }
}