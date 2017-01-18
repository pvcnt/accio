/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.domain

import fr.cnrs.liris.accio.testing.Operators
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GraphFactory]].
 */
class GraphFactorySpec extends UnitSpec {
  private val factory = {
    val opRegistry = new StaticOpRegistry(Operators.ops)
    new GraphFactory(opRegistry)
  }
  behavior of "GraphFactory"

  it should "populate node outputs" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Value(integers = Seq(42))))),
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple1",
        inputs = Map("foo" -> InputDef.Value(Value(integers = Seq(42))))),
      NodeDef(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> InputDef.Reference(Reference("FirstSimple", "data")),
          "data2" -> InputDef.Reference(Reference("FirstSimple1", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Value(doubles = Seq(3.14))),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data"))))
    ))
    val graph = factory.create(graphDef)

    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data1"),
      Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data2")))
  }

  it should "detect duplicate node name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Value(integers = Seq(42))))),
      NodeDef(
        op = "SecondSimple",
        name = "FirstSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Value(doubles = Seq(3.14))),
          "data" -> InputDef.Reference(Reference("FirstSimple", "data"))))
    ))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Duplicate node name: FirstSimple"
  }

  it should "detect unknown operator" in {
    val graphDef = GraphDef(Set(NodeDef(op = "InvalidOp", name = "MyOp")))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown operator: InvalidOp"
  }

  it should "detect unknown input name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map(
          "foo" -> InputDef.Value(Values.encodeInteger(42)),
          "bar" -> InputDef.Value(Values.encodeInteger(43))))
    ))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown inputs of FirstSimple: bar"
  }

  /*it should "detect invalid input type" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Value(strings = Seq("bar"))))
    ))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Invalid value for integer input FirstSimple/foo: bar"
  }*/

  it should "detect unknown input predecessor name" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("UnknownTesting", "data"))))))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown input predecessor: UnknownTesting"
  }

  it should "detect unknown input predecessor port" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("FirstSimple", "unknown"))))))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown input predecessor port: FirstSimple/unknown"
  }

  it should "detect missing roots" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "SecondSimple",
        name = "First",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("Second", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "Second",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("First", "data"))))))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "No root found"
  }

  it should "detect inconsistent data type" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "First",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "FirstSimple",
        name = "Second",
        inputs = Map("foo" -> InputDef.Reference(Reference("First", "data"))))))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Data type mismatch: First/data => Second/foo"
  }

  it should "detect missing input" in {
    val graphDef = GraphDef(Set(NodeDef(op = "FirstSimple", name = "FirstSimple")))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "No value for input FirstSimple/foo"
  }

  it should "detect invalid node name" in {
    val graphDef = GraphDef(Set(NodeDef(op = "FirstSimple", name = "First/Simple")))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage should startWith("Invalid node name: First/Simple")
  }

  it should "detect cycles" in {
    val graphDef = GraphDef(Set(
      NodeDef(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Map("foo" -> InputDef.Value(Values.encodeInteger(42)))),
      NodeDef(
        op = "ThirdSimple",
        name = "ThirdSimple",
        inputs = Map(
          "data1" -> InputDef.Reference(Reference("FirstSimple", "data")),
          "data2" -> InputDef.Reference(Reference("SecondSimple", "data")))),
      NodeDef(
        op = "SecondSimple",
        name = "SecondSimple",
        inputs = Map(
          "dbl" -> InputDef.Value(Values.encodeDouble(3.14)),
          "data" -> InputDef.Reference(Reference("ThirdSimple", "data"))))))
    val expected = intercept[InvalidGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Cycles found: ThirdSimple -> SecondSimple -> ThirdSimple"
  }
}