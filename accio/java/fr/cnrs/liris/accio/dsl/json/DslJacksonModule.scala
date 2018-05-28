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
import fr.cnrs.liris.accio.domain._
import fr.cnrs.liris.lumos.domain.{DataType, Value}

private[json] object DslJacksonModule extends SimpleModule {
  addDeserializer(classOf[Channel.Source], SourceDeserializer)
  addDeserializer(classOf[DataType], DataTypeDeserializer)
  addDeserializer(classOf[Value], ValueDeserializer)
}

private object SourceDeserializer extends StdDeserializer[Channel.Source](classOf[Channel.Source]) {

  private case class Reference(step: String, output: String)

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Channel.Source = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    val mapper = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    if (tree.has("param")) {
      Channel.Param(tree.get("param").asText)
    } else if (tree.has("constant")) {
      Channel.Constant(mapper.treeToValue(tree.get("constant"), classOf[Value]))
    } else if (tree.has("reference")) {
      val ref = mapper.treeToValue(tree.get("reference"), classOf[Reference])
      Channel.Reference(ref.step, ref.output)
    } else {
      throw ctx.mappingException(s"No discriminant field found, expected one of 'param', 'constant', 'reference'")
    }
  }
}

private object DataTypeDeserializer extends StdDeserializer[DataType](classOf[DataType]) {
  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): DataType = {
    DataType.parse(jsonParser.toString).getOrElse {
      throw ctx.mappingException(s"Invalid data type: ${jsonParser.toString}")
    }
  }
}

private object ValueDeserializer extends StdDeserializer[Value](classOf[Value]) {
  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Value = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    if (tree.isBoolean) {
      Value.Bool(tree.asBoolean)
    } else if (tree.isDouble) {
      Value.Double(tree.asDouble)
    } else if (tree.isInt) {
      Value.Int(tree.asInt)
    } else if (tree.isLong) {
      Value.Long(tree.asLong)
    } else if (tree.isTextual) {
      Value.String(tree.asText)
    } else {
      throw ctx.mappingException(s"Invalid node type: ${tree.getNodeType}")
    }
  }
}