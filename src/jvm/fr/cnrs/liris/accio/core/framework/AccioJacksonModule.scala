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

package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.{JsonInclude, JsonSubTypes}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.util.{Duration => TwitterDuration}
import fr.cnrs.liris.common.geo.Distance

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * Accio integration with Jackson. Configures Jackson to be able to (de)serialize all needed objects.
 */
object AccioFinatraJacksonModule extends FinatraJacksonModule {
  override protected val serializationInclusion = JsonInclude.Include.NON_ABSENT

  override protected def additionalJacksonModules: Seq[Module] = Seq(AccioJacksonModule)

  override protected def additionalMapperConfiguration(mapper: ObjectMapper): Unit = {
    mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, true)
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
  }
}

private object AccioJacksonModule extends SimpleModule {
  addSerializer(new DistanceSerializer)
  addSerializer(new TwitterDurationSerializer)
  addDeserializer(classOf[TwitterDuration], new TwitterDurationDeserializer)
  addDeserializer(classOf[Reference], new ReferenceDeserializer)
  addDeserializer(classOf[DataType], new DataTypeDeserializer)
  addDeserializer(classOf[User], new UserDeserializer)
  addDeserializer(classOf[GraphDef], new GraphDefDeserializer)
  addDeserializer(classOf[Graph], new GraphDeserializer)
  addDeserializer(classOf[Input], new InputDeserializer)
  addDeserializer(classOf[Exploration], new ExplorationDeserializer)
  //addDeserializer(classOf[Exploration], new PropertyPresentDeserializer[Exploration])
}

private class DistanceSerializer extends StdSerializer[Distance](classOf[Distance]) {
  override def serialize(obj: Distance, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeString(obj.toString)
  }
}

private class TwitterDurationSerializer extends StdSerializer[TwitterDuration](classOf[TwitterDuration]) {
  override def serialize(obj: TwitterDuration, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeNumber(obj.inMillis)
  }
}

private class TwitterDurationDeserializer extends StdDeserializer[TwitterDuration](classOf[TwitterDuration]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): TwitterDuration = {
    TwitterDuration.fromMilliseconds(jsonParser.getValueAsLong)
  }
}

private class GraphDefDeserializer extends StdDeserializer[GraphDef](classOf[GraphDef]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): GraphDef = {
    GraphDef(jsonParser.readValueAs[Seq[NodeDef]](new TypeReference[Seq[NodeDef]] {}))
  }
}

private class GraphDeserializer extends StdDeserializer[Graph](classOf[Graph]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Graph = {
    Graph(jsonParser.readValueAs[Seq[Node]](new TypeReference[Seq[Node]] {}).toSet)
  }
}

private class DataTypeDeserializer extends StdDeserializer[DataType](classOf[DataType]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): DataType = {
    val strValue = jsonParser.getValueAsString
    try {
      DataType.parse(strValue)
    } catch {
      case e: IllegalArgumentException => throw new InvalidFormatException(e.getMessage, strValue, classOf[DataType])
    }
  }
}

private class ReferenceDeserializer extends StdDeserializer[Reference](classOf[Reference]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Reference = {
    val strValue = jsonParser.getValueAsString
    try {
      Reference.parse(strValue)
    } catch {
      case e: IllegalArgumentException => throw new InvalidFormatException(e.getMessage, strValue, classOf[Reference])
    }
  }
}

private class UserDeserializer extends StdDeserializer[User](classOf[User]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): User = {
    val strValue = jsonParser.getValueAsString
    try {
      User.parse(strValue)
    } catch {
      case e: IllegalArgumentException => throw new InvalidFormatException(e.getMessage, strValue, classOf[User])
    }
  }
}

private class InputDeserializer extends StdDeserializer[Input](classOf[Input]) {
  private[this] val subTypes = _valueClass.getAnnotation(classOf[JsonSubTypes]).value

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): Input = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    if (tree.isObject) {
      JacksonUtils.deserialize[Input](tree, subTypes, ctx, jsonParser.getCodec.asInstanceOf[ObjectMapper])
    } else {
      ValueInput(JacksonUtils.toPojo(tree, ctx))
    }
  }
}

private class ExplorationDeserializer extends StdDeserializer[Exploration](classOf[Exploration]) {
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

private class PropertyPresentDeserializer[T: ClassTag] extends StdDeserializer[T](implicitly[ClassTag[T]].runtimeClass) {
  private[this] val subTypes = _valueClass.getAnnotation(classOf[JsonSubTypes]).value

  override def deserialize(jsonParser: JsonParser, ctx: DeserializationContext): T = {
    val tree = jsonParser.readValueAsTree[JsonNode]
    JacksonUtils.deserialize[T](tree, subTypes, ctx, jsonParser.getCodec.asInstanceOf[ObjectMapper])
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