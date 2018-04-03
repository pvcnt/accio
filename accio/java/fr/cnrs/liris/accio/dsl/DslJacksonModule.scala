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

package fr.cnrs.liris.accio.dsl

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, ObjectMapper}
import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift.{AtomicType, Value}

import scala.collection.JavaConverters._

object DslJacksonModule extends SimpleModule {
  addDeserializer(classOf[InputDsl], new InputDeserializer)
  addDeserializer(classOf[Value], new ValueDeserializer)
}

private class InputDeserializer extends StdDeserializer[InputDsl](classOf[InputDsl]) {
  private[this] val subTypes = _valueClass.getAnnotation(classOf[JsonSubTypes]).value

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): InputDsl = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    if (tree.isObject) {
      JacksonUtils.deserialize[InputDsl](tree, subTypes, ctx, jsonParser.getCodec.asInstanceOf[ObjectMapper])
    } else {
      InputDsl.Value(JacksonUtils.toValue(tree, ctx))
    }
  }
}

private class ValueDeserializer extends StdDeserializer[Value](classOf[Value]) {
  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Value = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    JacksonUtils.toValue(tree, ctx)
  }
}

private object JacksonUtils {
  def deserialize[T](tree: JsonNode, subTypes: Iterable[JsonSubTypes.Type], ctx: DeserializationContext, mapper: ObjectMapper): T = {
    subTypes.find(subType => tree.has(subType.name)) match {
      case Some(subType) => mapper.treeToValue(tree, subType.value).asInstanceOf[T]
      case None => throw ctx.mappingException(s"No required field found when deserializing, expected one of ${subTypes.map(_.name).mkString(", ")}")
    }
  }

  def toPojo(tree: JsonNode, ctx: DeserializationContext): Any = {
    if (tree.isBoolean) {
      tree.asBoolean
    } else if (tree.isDouble) {
      tree.asDouble
    } else if (tree.isInt) {
      tree.asInt
    } else if (tree.isLong) {
      tree.asLong
    } else if (tree.isTextual) {
      tree.asText
    } else if (tree.isArray) {
      tree.elements.asScala.toArray.map(toPojo(_, ctx))
    } else if (tree.isObject) {
      tree.fields.asScala.map(kv => kv.getKey -> toPojo(kv.getValue, ctx)).toMap
    } else {
      ctx.mappingException(s"Invalid node type for an input: ${tree.getNodeType}")
    }
  }

  def toValue(tree: JsonNode, ctx: DeserializationContext): Value = {
    if (tree.isBoolean) {
      Values.encodeBoolean(tree.asBoolean)
    } else if (tree.isDouble) {
      Values.encodeDouble(tree.asDouble)
    } else if (tree.isInt) {
      Values.encodeInteger(tree.asInt)
    } else if (tree.isLong) {
      Values.encodeLong(tree.asLong)
    } else if (tree.isTextual) {
      Values.encodeString(tree.asText)
    } else if (tree.isArray) {
      val items = tree.elements.asScala.toSeq
      if (items.isEmpty) {
        Values.emptyList
      } else {
        val of = toValue(items.head, ctx).kind.base
        Values
          .encodeList(items.map(toPojo(_, ctx)), of)
          .getOrElse(throw ctx.mappingException("Heterogeneous value types"))
      }
    } else if (tree.isObject) {
      val items = tree.fields.asScala.map(kv => kv.getKey -> kv.getValue).toMap
      if (items.isEmpty) {
        Values.emptyMap
      } else {
        val of = toValue(items.values.head, ctx).kind.base
        Values
          .encodeMap(items.map { case (k, v) => k -> toPojo(v, ctx) }, AtomicType.String, of)
          .getOrElse(throw ctx.mappingException("Heterogeneous value types"))
      }
    } else {
      throw ctx.mappingException(s"Invalid node type: ${tree.getNodeType}")
    }
  }
}