package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonNode}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}

import scala.reflect.{ClassTag, classTag}

class ScroogeUnionDeserializer[T <: ThriftStruct : ClassTag](codec: ThriftStructCodec[T]) extends StdDeserializer[T](classTag[T].runtimeClass.asInstanceOf[Class[T]]) {
  override def deserialize(jp: JsonParser, ctx: DeserializationContext): T = {
    val tree = jp.getCodec.readTree[JsonNode](jp)
    val maybeField = codec.metaData.unionFields.find(fieldInfo => tree.has(fieldInfo.structFieldInfo.tfield.name))
    maybeField match {
      case None => throw new RuntimeException(s"Cannot deserialize union from $tree")
      case Some(fieldInfo) =>
        val ctorArg = jp.getCodec.treeToValue(tree.get(fieldInfo.structFieldInfo.tfield.name), fieldInfo.structFieldInfo.manifest.runtimeClass)
        val ctor = fieldInfo.fieldClassTag.runtimeClass.getConstructors.head
        ctor.newInstance(ctorArg.asInstanceOf[AnyRef]).asInstanceOf[T]
    }
  }
}