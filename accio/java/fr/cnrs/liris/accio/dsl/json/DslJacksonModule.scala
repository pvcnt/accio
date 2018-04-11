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

package fr.cnrs.liris.accio.dsl.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode, ObjectMapper}
import fr.cnrs.liris.accio.api.thrift.AtomicType
import fr.cnrs.liris.accio.api.{Values, thrift}

import scala.collection.JavaConverters._

object DslJacksonModule extends SimpleModule {
  addDeserializer(classOf[thrift.Channel], ChannelDeserializer)
  addDeserializer(classOf[thrift.Value], ValueDeserializer)
}

private object ChannelDeserializer extends StdDeserializer[thrift.Channel](classOf[thrift.Channel]) {

  private case class Reference(step: String, output: String)

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): thrift.Channel = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    val mapper = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    if (tree.has("param")) {
      thrift.Channel.Param(tree.get("param").asText)
    } else if (tree.has("value")) {
      thrift.Channel.Value(mapper.treeToValue(tree.get("value"), classOf[thrift.Value]))
    } else if (tree.has("reference")) {
      val ref = mapper.treeToValue(tree.get("reference"), classOf[Reference])
      thrift.Channel.Reference(thrift.Reference(ref.step, ref.output))
    } else {
      throw ctx.mappingException(s"No discriminant field found, expected one of 'param', 'value', 'reference'")
    }
  }
}

private object ValueDeserializer extends StdDeserializer[thrift.Value](classOf[thrift.Value]) {
  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): thrift.Value = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    toValue(tree, ctx)
  }

  private def getAtomicType(tree: JsonNode, ctx: DeserializationContext): thrift.AtomicType = {
    if (tree.isBoolean) {
      thrift.AtomicType.Boolean
    } else if (tree.isDouble) {
      thrift.AtomicType.Double
    } else if (tree.isInt) {
      thrift.AtomicType.Integer
    } else if (tree.isLong) {
      thrift.AtomicType.Long
    } else if (tree.isTextual) {
      thrift.AtomicType.String
    } else {
      throw ctx.mappingException(s"Invalid node type: ${tree.getNodeType}")
    }
  }

  private def toValue(tree: JsonNode, ctx: DeserializationContext): thrift.Value = {
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
        val values = getAtomicType(items.head, ctx)
        Values
          .encodeList(items.map(toPojo(_, ctx)), values)
          .getOrElse(throw ctx.mappingException("Heterogeneous value types"))
      }
    } else if (tree.isObject) {
      val items = tree.fields.asScala.map(kv => kv.getKey -> kv.getValue).toMap
      if (items.isEmpty) {
        Values.emptyMap
      } else {
        val values = getAtomicType(items.values.head, ctx)
        Values
          .encodeMap(items.map { case (k, v) => k -> toPojo(v, ctx) }, AtomicType.String, values)
          .getOrElse(throw ctx.mappingException("Heterogeneous value types"))
      }
    } else {
      throw ctx.mappingException(s"Invalid node type: ${tree.getNodeType}")
    }
  }

  private def toPojo(tree: JsonNode, ctx: DeserializationContext): Any = {
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
}