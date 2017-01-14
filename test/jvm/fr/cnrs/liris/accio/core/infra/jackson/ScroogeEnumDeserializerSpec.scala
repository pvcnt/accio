package fr.cnrs.liris.accio.core.infra.jackson

class ScroogeEnumDeserializerSpec extends ScroogeSpec {
  behavior of "ScroogeEnumDeserializer"

  it should "deserialize an enum" in {
    mapper.readValue( "0", classOf[TestEnum]) shouldBe TestEnum.Foo
    mapper.readValue( "1", classOf[TestEnum]) shouldBe TestEnum.Bar
    mapper.readValue( "2", classOf[TestEnum]) shouldBe TestEnum.Foobar
  }
}
