package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec
import scala.reflect.runtime.universe._

/**
 * Unit tests for [[FlagsParser]].
 */
class FlagsParserSpec extends UnitSpec {
  "FlagsParser" should "parse with multiple options interfaces" in {
    val parser = FlagsParser(typeOf[ExampleFoo], typeOf[ExampleBaz])
    parser.parse(Seq("-baz=oops", "-bar", "17"))
    val foo = parser.as[ExampleFoo]
    foo.foo shouldBe "defaultFoo"
    foo.bar shouldBe 17
    val baz = parser.as[ExampleBaz]
    baz.baz shouldBe "oops"
    parser.residue should have size 0
  }

  it should "fail parsing with unknown option" in {
    val parser = FlagsParser(typeOf[ExampleFoo], typeOf[ExampleBaz])
    val e = intercept[FlagsParsingException] {
      parser.parse(Seq("-unknown", "option"))
    }
    e.invalidArgument shouldBe Some("-unknown")
    parser.as[ExampleFoo] shouldBe an[ExampleFoo]
    parser.as[ExampleBaz] shouldBe an[ExampleBaz]
    parser.residue should have size 0
  }

  it should "parse known and unknown options" in {
    val parser = FlagsParser(typeOf[ExampleFoo], typeOf[ExampleBaz])
    val e = intercept[FlagsParsingException] {
      parser.parse(Seq("-bar", "17", "-unknown", "option"))
    }
    e.invalidArgument shouldBe Some("-unknown")
    val foo = parser.as[ExampleFoo]
    foo.bar shouldBe 17
    parser.as[ExampleBaz] shouldBe an[ExampleBaz]
    parser.residue should have size 0
  }

  it should "return flags and residue with no call to parse" in {
    val parser = FlagsParser(typeOf[ExampleFoo])
    parser.as[ExampleFoo].foo shouldBe "defaultFoo"
    parser.residue should have size 0
  }

  it should "support being called repeatedly" in {
    val parser = FlagsParser(typeOf[ExampleFoo])
    parser.parse(Seq("-foo", "foo1"))
    parser.as[ExampleFoo].foo shouldBe "foo1"
    parser.parse(Seq.empty)
    parser.as[ExampleFoo].foo shouldBe "foo1" // no change
    parser.parse(Seq("-foo", "foo2"))
    parser.as[ExampleFoo].foo shouldBe "foo2" // updated
  }

  it should "support being called repeatedly with residue" in {
    val parser = FlagsParser(allowResidue = true, typeOf[ExampleFoo])
    parser.parse(Seq("-foo", "one", "-bar", "43", "unknown1"))
    parser.parse(Seq("-foo", "two", "unknown2"))
    val foo = parser.as[ExampleFoo]
    foo.foo shouldBe "two" // second call takes precedence
    foo.bar shouldBe 43
    parser.residue shouldBe Seq("unknown1", "unknown2")
  }

  it should "ignore flags after --" in {
    val parser = FlagsParser(typeOf[ExampleFoo], typeOf[ExampleBaz])
    parser.parse(Seq("-foo", "well", "-baz", "here", "--", "-bar", "ignore"))
    val foo = parser.as[ExampleFoo]
    val baz = parser.as[ExampleBaz]
    foo.foo shouldBe "well"
    baz.baz shouldBe "here"
    foo.bar shouldBe 42 // the default!
    parser.residue shouldBe Seq("-bar", "ignore")
  }

  it should "throw an exception if residue is not allowed" in {
    val parser = FlagsParser(allowResidue = false, typeOf[ExampleFoo])
    an[FlagsParsingException] shouldBe thrownBy {
      parser.parse(Seq("residue", "is", "not", "OK"))
    }
  }
}

private case class ExampleFoo(
    @Flag(name = "foo", category = "one")
    foo: String = "defaultFoo",
    @Flag(name = "bar", category = "two")
    bar: Int = 42,
    @Flag(name = "nodoc", category = "undocumented")
    nodoc: String = ""
)

private case class ExampleBaz(
    @Flag(name = "baz", category = "one")
    baz: String = "defaultBaz"
)

private case class CategoryTest(
    @Flag(name = "swiss_bank_account_number", category = "undocumented")
    swissBankAccountNumber: Int = 123456789,
    @Flag(name = "student_bank_account_number", category = "one")
    studentBankAccountNumber: Int = 987654321
)