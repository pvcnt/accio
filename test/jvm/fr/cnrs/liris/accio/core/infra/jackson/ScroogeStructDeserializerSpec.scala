package fr.cnrs.liris.accio.core.infra.jackson

/**
 * Unit tests of [[ScroogeStructDeserializer]].
 */
class ScroogeStructDeserializerSpec extends ScroogeSpec {
  behavior of "ScroogeStructDeserializer"

  it should "deserialize simple struct" in {
    val json = "{\"s\":\"bar\",\"i\":45}"
    val struct = mapper.readValue(json, classOf[NestedStruct])
    struct shouldBe NestedStruct(s = "bar", i = Some(45))
  }

  it should "deserialize complex struct" in {
    val json = "{\"s\":\"foobar\",\"i2\":42,\"l\":43,\"d\":3.14,\"en\":\"foo\",\"b2\":true,\"n\":{\"s\":\"bar\",\"i\":44},\"nl\":[{\"s\":\"bar\",\"i\":45},{\"s\":\"bar\"}]}"
    val struct = mapper.readValue(json, classOf[BigStruct])
    struct shouldBe BigStruct("foobar", Some(42), 43, 3.14, TestEnum.Foo, Some(true), Some(NestedStruct("bar", Some(44))), Seq(NestedStruct("bar", Some(45)), NestedStruct("bar")))
  }

  it should "deserialize strings" in {
    val json = "{\"one\":\"foo\",\"two\":\"bar\"}"
    val struct = mapper.readValue(json, classOf[StructWithStrings])
    struct shouldBe StructWithStrings("foo", Some("bar"))
  }

  it should "deserialize booleans" in {
    val json = "{\"one\":true,\"two\":false}"
    val struct = mapper.readValue(json, classOf[StructWithBooleans])
    struct shouldBe StructWithBooleans(true, Some(false))
  }

  it should "deserialize bytes" in {
    val json = "{\"one\":1,\"two\":2}"
    val struct = mapper.readValue(json, classOf[StructWithBytes])
    struct shouldBe StructWithBytes(1, Some(2))
  }

  it should "deserialize shorts" in {
    val json = "{\"one\":1,\"two\":2}"
    val struct = mapper.readValue(json, classOf[StructWithShorts])
    struct shouldBe StructWithShorts(1, Some(2))
  }

  it should "deserialize ints" in {
    val json = "{\"one\":1,\"two\":2}"
    val struct = mapper.readValue(json, classOf[StructWithInts])
    struct shouldBe StructWithInts(1, Some(2))
  }

  it should "deserialize longs" in {
    val json = "{\"one\":1,\"two\":2}"
    val struct = mapper.readValue(json, classOf[StructWithLongs])
    struct shouldBe StructWithLongs(1, Some(2))
  }

  it should "deserialize doubles" in {
    val json = "{\"one\":3.14,\"two\":3.15}"
    val struct = mapper.readValue(json, classOf[StructWithDoubles])
    struct shouldBe StructWithDoubles(3.14, Some(3.15))
  }

  it should "deserialize nested structs" in {
    val json = "{\"one\":{\"s\":\"bar\",\"i\":45},\"two\":{\"s\":\"bar\",\"i\":45}}"
    val struct = mapper.readValue(json, classOf[StructWithStructs])
    struct shouldBe StructWithStructs(NestedStruct(s = "bar", i = Some(45)), Some(NestedStruct(s = "bar", i = Some(45))))
  }

  it should "deserialize lists" in {
    val json = "{\"one\":[1,2,3],\"two\":[1,2,3]}"
    val struct = mapper.readValue(json, classOf[StructWithLists])
    struct shouldBe StructWithLists(one = Seq(1, 2, 3), two = Some(Seq(1, 2, 3)))
  }

  it should "deserialize sets" in {
    val json = "{\"one\":[1,2,3],\"two\":[1,2,3]}"
    val struct = mapper.readValue(json, classOf[StructWithSets])
    struct shouldBe StructWithSets(one = Set(1, 2, 3), two = Some(Set(1, 2, 3)))
  }

  it should "deserialize maps" in {
    val json = "{\"one_byte\":{\"1\":1,\"2\":2},\"two_byte\":{\"1\":1,\"2\":2}," +
      "\"one_bool\":{\"true\":1,\"false\":2},\"two_bool\":{\"true\":1,\"false\":2}," +
      "\"one_short\":{\"1\":1,\"2\":2},\"two_short\":{\"1\":1,\"2\":2}," +
      "\"one_int\":{\"1\":1,\"2\":2},\"two_int\":{\"1\":1,\"2\":2}," +
      "\"one_long\":{\"1\":1,\"2\":2},\"two_long\":{\"1\":1,\"2\":2}," +
      "\"one_double\":{\"1.1\":1,\"2.2\":2},\"two_double\":{\"1.1\":1,\"2.2\":2}," +
      "\"one_string\":{\"one\":1,\"two\":2},\"two_string\":{\"one\":1,\"two\":2}" +
      "}"
    val struct = mapper.readValue(json, classOf[StructWithMaps])
    struct shouldBe StructWithMaps(
      oneByte = Map(1.toByte -> 1, 2.toByte -> 2), twoByte = Some(Map(1.toByte -> 1, 2.toByte -> 2)),
      oneBool = Map(true -> 1, false -> 2), twoBool = Some(Map(true -> 1, false -> 2)),
      oneShort = Map(1.toShort -> 1, 2.toShort -> 2), twoShort = Some(Map(1.toShort -> 1, 2.toShort -> 2)),
      oneInt = Map(1 -> 1, 2 -> 2), twoInt = Some(Map(1 -> 1, 2 -> 2)),
      oneLong = Map(1L -> 1, 2L -> 2), twoLong = Some(Map(1L -> 1, 2L -> 2)),
      oneDouble = Map(1.1 -> 1, 2.2 -> 2), twoDouble = Some(Map(1.1 -> 1, 2.2 -> 2)),
      oneString = Map("one" -> 1, "two" -> 2), twoString = Some(Map("one" -> 1, "two" -> 2)))
  }
}