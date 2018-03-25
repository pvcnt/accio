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
import com.google.common.annotations.VisibleForTesting

import scala.collection.JavaConverters._

object DslJacksonModule extends SimpleModule {
  addDeserializer(classOf[JsonInputDef], new InputDeserializer)
  addDeserializer(classOf[Exploration], new ExplorationDeserializer)
}

@VisibleForTesting
private[dsl] class InputDeserializer extends StdDeserializer[JsonInputDef](classOf[JsonInputDef]) {
  private[this] val subTypes = _valueClass.getAnnotation(classOf[JsonSubTypes]).value

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): JsonInputDef = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    if (tree.isObject) {
      JacksonUtils.deserialize[JsonInputDef](tree, subTypes, ctx, jsonParser.getCodec.asInstanceOf[ObjectMapper])
    } else {
      JsonValueInputDef(JacksonUtils.toPojo(tree, ctx))
    }
  }
}

@VisibleForTesting
private[dsl] class ExplorationDeserializer extends StdDeserializer[Exploration](classOf[Exploration]) {
  private[this] val subTypes = _valueClass.getAnnotation(classOf[JsonSubTypes]).value

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Exploration = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    if (tree.isObject) {
      JacksonUtils.deserialize[Exploration](tree, subTypes, ctx, jsonParser.getCodec.asInstanceOf[ObjectMapper])
    } else {
      SingletonExploration(JacksonUtils.toPojo(tree, ctx))
    }
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
}