package fr.cnrs.liris.accio.core.infra.jackson

import com.fasterxml.jackson.databind.module.SimpleModule

object ScroogeModule extends SimpleModule {
  addDeserializer(classOf[BigStruct], new ScroogeStructDeserializer(BigStruct))
  addDeserializer(classOf[NestedStruct], new ScroogeStructDeserializer(NestedStruct))
  addDeserializer(classOf[StructWithStrings], new ScroogeStructDeserializer[StructWithStrings](StructWithStrings))
  addDeserializer(classOf[StructWithBooleans], new ScroogeStructDeserializer[StructWithBooleans](StructWithBooleans))
  addDeserializer(classOf[StructWithBytes], new ScroogeStructDeserializer[StructWithBytes](StructWithBytes))
  addDeserializer(classOf[StructWithShorts], new ScroogeStructDeserializer[StructWithShorts](StructWithShorts))
  addDeserializer(classOf[StructWithInts], new ScroogeStructDeserializer[StructWithInts](StructWithInts))
  addDeserializer(classOf[StructWithLongs], new ScroogeStructDeserializer[StructWithLongs](StructWithLongs))
  addDeserializer(classOf[StructWithDoubles], new ScroogeStructDeserializer[StructWithDoubles](StructWithDoubles))
  addDeserializer(classOf[StructWithStructs], new ScroogeStructDeserializer[StructWithStructs](StructWithStructs))
  addDeserializer(classOf[StructWithLists], new ScroogeStructDeserializer[StructWithLists](StructWithLists))
  addDeserializer(classOf[StructWithSets], new ScroogeStructDeserializer[StructWithSets](StructWithSets))
  addDeserializer(classOf[StructWithMaps], new ScroogeStructDeserializer[StructWithMaps](StructWithMaps))
  addDeserializer(classOf[TestEnum], new ScroogeEnumDeserializer[TestEnum](TestEnum))
  addDeserializer(classOf[TestUnion], new ScroogeUnionDeserializer[TestUnion](TestUnion))

  addSerializer(new ScroogeStructSerializer)
  addSerializer(new ScroogeEnumSerializer)
}
