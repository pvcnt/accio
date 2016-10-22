/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[GraphFactory]].
 */
class GraphFactorySpec extends UnitSpec {
  val factory = {
    val reader = new ReflectOpMetaReader
    val registry = new OpRegistry(reader, Set(classOf[FirstSimpleOp], classOf[SecondSimpleOp], classOf[ThirdSimpleOp]))
    new GraphFactory(registry)
  }

  behavior of "GraphFactory"

  it should "populate node outputs" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "FirstSimple",
        inputs = Map(
          "foo" -> ValueInput(42))),
      NodeDef(
        op = "FirstSimple",
        customName = Some("FirstSimple1"),
        inputs = Map(
          "foo" -> ValueInput(42))),
      NodeDef(
        op = "ThirdSimple",
        inputs = Map(
          "data1" -> ReferenceInput(Reference("FirstSimple", "data")),
          "data2" -> ReferenceInput(Reference("FirstSimple1", "data")))),
      NodeDef(
        op = "SecondSimple",
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("FirstSimple", "data"))))))
    val graph = factory.create(graphDef)

    graph("FirstSimple").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data1"),
      Reference("SecondSimple", "data")))
    graph("FirstSimple1").outputs should contain theSameElementsAs Map("data" -> Set(
      Reference("ThirdSimple", "data2")))
  }

  it should "detect duplicate node name" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "FirstSimple",
        inputs = Map("foo" -> ValueInput(42))),
      NodeDef(
        op = "SecondSimple",
        customName = Some("FirstSimple"),
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("FirstSimple", "data"))))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Duplicate node name: FirstSimple"
  }

  it should "detect unknown operator" in {
    val graphDef = GraphDef(Seq(NodeDef(op = "InvalidOp")))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown operator: InvalidOp"
  }

  it should "detect unknown input name" in {
    val graphDef = GraphDef(Seq(NodeDef(op = "FirstSimple", inputs = Map("foo" -> ValueInput(42), "bar" -> ValueInput(43)))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown inputs of FirstSimple: bar"
  }

  it should "detect invalid input type" in {
    val graphDef = GraphDef(Seq(NodeDef(op = "FirstSimple", inputs = Map("foo" -> ValueInput("bar")))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Invalid value for integer input FirstSimple/foo: bar"
  }

  it should "detect unknown input predecessor name" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "FirstSimple",
        inputs = Map("foo" -> ValueInput(42))),
      NodeDef(
        op = "SecondSimple",
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("UnknownTesting", "data"))))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown input predecessor: UnknownTesting/data"
  }

  it should "detect unknown input predecessor port" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "FirstSimple",
        inputs = Map("foo" -> ValueInput(42))),
      NodeDef(
        op = "SecondSimple",
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("FirstSimple", "unknown"))))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Unknown input predecessor port: FirstSimple/unknown"
  }

  it should "detect missing roots" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "SecondSimple",
        customName = Some("First"),
        inputs = Map(
          "dbl" -> ValueInput(42),
          "data" -> ReferenceInput(Reference("Second", "data")))),
      NodeDef(
        op = "SecondSimple",
        customName = Some("Second"),
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("First", "data"))))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "No root found"
  }

  it should "detect cycles" in {
    val graphDef = GraphDef(Seq(
      NodeDef(
        op = "FirstSimple",
        inputs = Map(
          "foo" -> ValueInput(42))),
      NodeDef(
        op = "ThirdSimple",
        inputs = Map(
          "data1" -> ReferenceInput(Reference("FirstSimple", "data")),
          "data2" -> ReferenceInput(Reference("SecondSimple", "data")))),
      NodeDef(
        op = "SecondSimple",
        inputs = Map(
          "dbl" -> ValueInput(3.14),
          "data" -> ReferenceInput(Reference("ThirdSimple", "data"))))))
    val expected = intercept[IllegalGraphException] {
      factory.create(graphDef)
    }
    expected.getMessage shouldBe "Cycles found: ThirdSimple -> SecondSimple -> ThirdSimple"
  }
}

private case class FirstSimpleIn(@Arg foo: Int)

private case class FirstSimpleOut(@Arg data: Dataset)

@Op
private class FirstSimpleOp extends Operator[FirstSimpleIn, FirstSimpleOut] {
  override def execute(in: FirstSimpleIn, ctx: OpContext): FirstSimpleOut = FirstSimpleOut(Dataset("/dev/null", "csv"))
}

private case class SecondSimpleIn(@Arg dbl: Double, @Arg str: String = "something", @Arg data: Dataset)

private case class SecondSimpleOut(@Arg data: Dataset)

@Op
private class SecondSimpleOp extends Operator[SecondSimpleIn, SecondSimpleOut] {
  override def execute(in: SecondSimpleIn, ctx: OpContext): SecondSimpleOut = SecondSimpleOut(in.data)
}

private case class ThirdSimpleIn(@Arg data1: Dataset, @Arg data2: Dataset)

private case class ThirdSimpleOut(@Arg data: Dataset)

@Op
private class ThirdSimpleOp extends Operator[ThirdSimpleIn, ThirdSimpleOut] {
  override def execute(in: ThirdSimpleIn, ctx: OpContext): ThirdSimpleOut = ThirdSimpleOut(in.data2)
}