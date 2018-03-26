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

package fr.cnrs.liris.common.flags

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[FlagsParser]].
 */
class FlagsParserSpec extends UnitSpec {
  behavior of "FlagsParser"

  it should "parse multiple flags classes" in {
    val parser = FlagsParser(allowResidue = false, classOf[FooFlags], classOf[BazFlags])
    parser.parse(Seq("-baz=oops", "-bar", "17"))
    val foo = parser.as[FooFlags]
    foo.foo shouldBe "defaultFoo"
    foo.bar shouldBe 17
    val baz = parser.as[BazFlags]
    baz.baz shouldBe "oops"
    parser.residue should have size 0
  }

  it should "fail parsing an unknown flag" in {
    val parser = FlagsParser(allowResidue = false, classOf[FooFlags], classOf[BazFlags])
    val e = intercept[FlagsParsingException] {
      parser.parse(Seq("-unknown", "option"))
    }
    e.invalidArgument shouldBe Some("-unknown")
    parser.as[FooFlags] shouldBe an[FooFlags]
    parser.as[BazFlags] shouldBe an[BazFlags]
  }

  it should "parse known and unknown flags" in {
    val parser = FlagsParser(allowResidue = true, classOf[FooFlags], classOf[BazFlags])
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
    val parser = FlagsParser(allowResidue = true, classOf[FooFlags])
    parser.as[FooFlags].foo shouldBe "defaultFoo"
    parser.residue should have size 0
  }

  it should "support being called repeatedly" in {
    val parser = FlagsParser(allowResidue = false, classOf[FooFlags])
    parser.parse(Seq("-foo", "foo1"))
    parser.as[FooFlags].foo shouldBe "foo1"
    parser.parse(Seq.empty)
    parser.as[FooFlags].foo shouldBe "foo1" // no change
    parser.parse(Seq("-foo", "foo2"))
    parser.as[FooFlags].foo shouldBe "foo2" // updated
  }

  it should "support being called repeatedly with residue" in {
    val parser = FlagsParser(allowResidue = true, classOf[FooFlags])
    parser.parse(Seq("-foo", "one", "-bar", "43", "unknown1"))
    parser.parse(Seq("-foo", "two", "unknown2"))
    val foo = parser.as[FooFlags]
    foo.foo shouldBe "two" // second call takes precedence
    foo.bar shouldBe 43
    parser.residue shouldBe Seq("unknown1", "unknown2")
  }

  it should "ignore flags after --" in {
    val parser = FlagsParser(allowResidue = true, classOf[FooFlags], classOf[BazFlags])
    parser.parse(Seq("-foo", "well", "-baz", "here", "--", "-bar", "ignore"))
    val foo = parser.as[FooFlags]
    val baz = parser.as[BazFlags]
    foo.foo shouldBe "well"
    baz.baz shouldBe "here"
    foo.bar shouldBe 42 // the default!
    parser.residue shouldBe Seq("-bar", "ignore")
  }

  it should "throw an exception if residue is not allowed" in {
    val parser = FlagsParser(allowResidue = false, classOf[FooFlags])
    an[FlagsParsingException] shouldBe thrownBy {
      parser.parse(Seq("residue", "is", "not", "OK"))
    }
  }

  it should "support optional values" in {
    val parser = FlagsParser(allowResidue = false, classOf[OptionalFlags])
    parser.parse(Seq("-bar", "barbar"))
    val opt = parser.as[OptionalFlags]
    opt.foo shouldBe None
    opt.bar shouldBe Some("barbar")
    opt.baz shouldBe Some("bazbaz") // Although there is no reason to define an optional flag with default value!
  }
}
