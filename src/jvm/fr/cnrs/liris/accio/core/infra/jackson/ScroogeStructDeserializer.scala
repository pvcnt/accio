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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec, ThriftStructFieldInfo}
import org.apache.thrift.protocol.TType

import scala.collection.JavaConverters._
import scala.reflect._

final class ScroogeStructDeserializer[T <: ThriftStruct : ClassTag](codec: ThriftStructCodec[T]) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
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
      case unsupported => throw new RuntimeException(s"Unsupported key class: $unsupported")
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
          throw new RuntimeException(s"No value nor default value for field ${fieldInfo.tfield.name}")
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
      case unknown => throw new RuntimeException(s"Unknown type: $unknown")
    }
    if (fieldInfo.isOptional) Some(value) else value
  }

  private def parseBoolean(str: String) = str match {
    case "1" => true
    case "0" => false
    case _ => str.toBoolean
  }
}

