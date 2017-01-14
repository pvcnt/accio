package fr.cnrs.liris.accio.core.infra.jackson

/**
 * Unit tests of [[ScroogeUnionDeserializer]].
 */
class ScroogeUnionDeserializerSpec extends ScroogeSpec {
  behavior of "ScroogeUnionDeserializer"

  it should "deserialize a union" in {
    val json = "{\"s\":\"foo\"}"
    mapper.readValue(json, classOf[TestUnion]) shouldBe TestUnion.S("foo")
  }
}
