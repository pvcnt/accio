package fr.cnrs.liris.accio.core.pipeline

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import fr.cnrs.liris.accio.core.framework.ParamMap

private[pipeline] class JacksonAccioModule extends SimpleModule {
  addSerializer(new ParamMapSerializer)
  addSerializer(new UserSerializer)
  addDeserializer(classOf[ParamMap], new ParamMapDeserializer)
  addDeserializer(classOf[User], new UserDeserializer)
}

private class ParamMapSerializer extends StdSerializer[ParamMap](classOf[ParamMap]) {
  override def serialize(paramMap: ParamMap, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeObject(paramMap.toMap)
  }
}

private class ParamMapDeserializer extends StdDeserializer[ParamMap](classOf[ParamMap]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): ParamMap = {
    val map = jsonParser.readValueAs(classOf[Map[String, Any]])
    new ParamMap(map)
  }
}

private class UserSerializer extends StdSerializer[User](classOf[User]) {
  override def serialize(user: User, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeString(user.toString)
  }
}

private class UserDeserializer extends StdDeserializer[User](classOf[User]) {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): User = {
    User.parse(jsonParser.getValueAsString)
  }
}

/*private class NodeDefDeserializer extends StdDeserializer[NodeDef](classOf[NodeDef]) {

  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): NodeDef = {
    jsonParser.getCodec.
  }
}*/