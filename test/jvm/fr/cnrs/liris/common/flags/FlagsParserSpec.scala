package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[FlagsParser]].
 */
class FlagsParserSpec extends UnitSpec {
  val factory = {
    val converters = Set[Converter[_]](
      new ByteConverter,
      new ShortConverter,
      new IntConverter,
      new LongConverter,
      new DoubleConverter,
      new StringConverter,
      new PathConverter,
      new BooleanConverter,
      new TriStateConverter(new BooleanConverter))
    new FlagsParserFactory(converters)
  }

  behavior of "FlagsParser"

  it should "parse multiple flags classes" in {
    val parser = factory.create(classOf[FooFlags], classOf[BazFlags])
    parser.parse(Seq("-baz=oops", "-bar", "17"))
    val foo = parser.as[FooFlags]
    foo.foo shouldBe "defaultFoo"
    foo.bar shouldBe 17
    val baz = parser.as[BazFlags]
    baz.baz shouldBe "oops"
    parser.residue should have size 0
  }

  it should "fail parsing an unknown flag" in {
    val parser = factory.create(classOf[FooFlags], classOf[BazFlags])
    val e = intercept[FlagsParsingException] {
      parser.parse(Seq("-unknown", "option"))
    }
    e.invalidArgument shouldBe Some("-unknown")
    parser.as[FooFlags] shouldBe an[FooFlags]
    parser.as[BazFlags] shouldBe an[BazFlags]
    parser.residue should have size 0
  }

  it should "parse known and unknown flags" in {
    val parser = factory.create(classOf[FooFlags], classOf[BazFlags])
    val e = intercept[FlagsParsingException] {
      parser.parse(Seq("-bar", "17", "-unknown", "option"))
    }
    e.invalidArgument shouldBe Some("-unknown")

    val foo = parser.as[FooFlags]
    foo.bar shouldBe 17
    parser.as[BazFlags] shouldBe an[BazFlags]
    parser.residue should have size 0
  }

  it should "return flags and residue with no call to parse" in {
    val parser = factory.create(classOf[FooFlags])
    parser.as[FooFlags].foo shouldBe "defaultFoo"
    parser.residue should have size 0
  }

  it should "support being called repeatedly" in {
    val parser = factory.create(classOf[FooFlags])
    parser.parse(Seq("-foo", "foo1"))
    parser.as[FooFlags].foo shouldBe "foo1"
    parser.parse(Seq.empty)
    parser.as[FooFlags].foo shouldBe "foo1" // no change
    parser.parse(Seq("-foo", "foo2"))
    parser.as[FooFlags].foo shouldBe "foo2" // updated
  }

  it should "support being called repeatedly with residue" in {
    val parser = factory.create(allowResidue = true, classOf[FooFlags])
    parser.parse(Seq("-foo", "one", "-bar", "43", "unknown1"))
    parser.parse(Seq("-foo", "two", "unknown2"))
    val foo = parser.as[FooFlags]
    foo.foo shouldBe "two" // second call takes precedence
    foo.bar shouldBe 43
    parser.residue shouldBe Seq("unknown1", "unknown2")
  }

  it should "ignore flags after --" in {
    val parser = factory.create(classOf[FooFlags], classOf[BazFlags])
    parser.parse(Seq("-foo", "well", "-baz", "here", "--", "-bar", "ignore"))
    val foo = parser.as[FooFlags]
    val baz = parser.as[BazFlags]
    foo.foo shouldBe "well"
    baz.baz shouldBe "here"
    foo.bar shouldBe 42 // the default!
    parser.residue shouldBe Seq("-bar", "ignore")
  }

  it should "throw an exception if residue is not allowed" in {
    val parser = factory.create(allowResidue = false, classOf[FooFlags])
    an[FlagsParsingException] shouldBe thrownBy {
      parser.parse(Seq("residue", "is", "not", "OK"))
    }
  }

  it should "support optional values" in {
    val parser = factory.create(allowResidue = false, classOf[OptionalFlags])
    parser.parse(Seq("-bar", "barbar"))
    val opt = parser.as[OptionalFlags]
    opt.foo shouldBe None
    opt.bar shouldBe Some("barbar")
    opt.baz shouldBe Some("bazbaz") // Although there is no reason to define an optional flag with default value!
  }
}
