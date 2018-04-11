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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.{FieldViolation, NamedChannel}
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance

/**
 * Unit tests for [[JobValidator]].
 */
class JobValidatorSpec extends UnitSpec {
  behavior of "JobValidator"

  private val validator = new JobValidator(new OpRegistry(Operators.ops))

  it should "validate a legitimate job" in {
    val obj = thrift.Job(
      name = "valid-job",
      steps = Seq(thrift.Step(
        op = "FirstSimple",
        name = "FirstSimple",
        inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple1",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "ThirdSimple",
          name = "ThirdSimple",
          inputs = Seq(
            NamedChannel("data1", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data"))),
            NamedChannel("data2", thrift.Channel.Reference(thrift.Reference("FirstSimple1", "data"))))),
        thrift.Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe true
    res.errors should have size 0
    res.warnings should have size 0
  }

  it should "reject a duplicate step name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "SecondSimple",
          name = "FirstSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain(FieldViolation("Duplicate step name: FirstSimple (appears 2 times)", "steps.0.name"))
    res.errors should contain(FieldViolation("Duplicate step name: FirstSimple (appears 2 times)", "steps.1.name"))
  }

  it should "reject an unknown operator" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(thrift.Step(op = "InvalidOp", name = "MyOp")))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown operator: InvalidOp", "steps.0.op"))
  }

  it should "detect an unknown input name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(
            NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))),
            NamedChannel("bar", thrift.Channel.Value(Values.encodeInteger(43)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe true
    res.warnings should contain theSameElementsAs Set(FieldViolation("Unknown parameter for operator FirstSimple: bar", "steps.0.inputs.bar"))
  }

  it should "reject an invalid operator param type" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeString("bar")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires integer, got string", "steps.0.inputs.foo.value"))
  }

  it should "reject an unknown reference step name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("UnknownTesting", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown step: UnknownTesting", "steps.1.inputs.data.step"))
  }

  it should "reject an unknown reference port name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("FirstSimple", "unknown")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown output for operator FirstSimple: FirstSimple/unknown", "steps.1.inputs.data.port"))
  }

  it should "reject a job without any roots" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "SecondSimple",
          name = "First",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("Second", "data"))))),
        thrift.Step(
          op = "SecondSimple",
          name = "Second",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("First", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("No root step", "steps"))
  }

  it should "reject inconsistent data types in references" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "First",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "FirstSimple",
          name = "Second",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Reference(thrift.Reference("First", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires integer, got dataset()", "steps.1.inputs.foo"))
  }

  it should "reject a missing parameter" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(thrift.Step(op = "FirstSimple", name = "FirstSimple")))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Required parameter for operator FirstSimple is missing: foo", "steps.0.inputs"))
  }

  it should "reject an invalid step name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "First/Simple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Illegal name: First/Simple (should match [a-zA-Z][a-zA-Z0-9-]+)", "steps.0.name"))
  }

  it should "reject a cyclic graph" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42))))),
        thrift.Step(
          op = "ThirdSimple",
          name = "ThirdSimple",
          inputs = Seq(
            NamedChannel("data1", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data"))),
            NamedChannel("data2", thrift.Channel.Reference(thrift.Reference("SecondSimple", "data"))))),
        thrift.Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Value(Values.encodeDouble(3.14))),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("ThirdSimple", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Cycle detected: ThirdSimple -> SecondSimple -> ThirdSimple", "steps"))
  }

  it should "detect a deprecated operator" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "Deprecated",
          name = "Deprecated",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(42)))))))
    val res = validator.validate(obj)
    res.warnings should contain(FieldViolation("Operator Deprecated is deprecated: Do not use it!", "steps.0.op"))
  }

  it should "detect an invalid job name" in {
    val obj = thrift.Job(
      name = "workflow!id",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Value(Values.encodeInteger(2)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Illegal name: workflow!id (should match [a-zA-Z][a-zA-Z0-9-]+)", "name"))
  }

  it should "reject a param type mismatch" in {
    val obj = thrift.Job(
      name = "invalid-job",
      params = Seq(thrift.NamedValue("foo", Values.encodeDistance(Distance.meters(42)))),
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Param("foo")))),
        thrift.Step(
          op = "SecondSimple",
          name = "SecondSimple",
          inputs = Seq(
            NamedChannel("dbl", thrift.Channel.Param("foo")),
            NamedChannel("data", thrift.Channel.Reference(thrift.Reference("FirstSimple", "data")))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Data type mismatch: requires integer, got distance", "steps.0.inputs.foo.param"),
      FieldViolation("Data type mismatch: requires double, got distance", "steps.1.inputs.dbl.param"))
  }

  it should "reject an invalid param name" in {
    val obj = thrift.Job(
      name = "invalid-job",
      params = Seq(thrift.NamedValue("foo/foo", Values.encodeInteger(1))),
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Param("foo/foo"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Illegal name: foo/foo (should match [a-zA-Z][a-zA-Z0-9-]+)", "params.0.name"))
  }

  it should "reject an undeclared param" in {
    val obj = thrift.Job(
      name = "invalid-job",
      steps = Seq(
        thrift.Step(
          op = "FirstSimple",
          name = "FirstSimple",
          inputs = Seq(NamedChannel("foo", thrift.Channel.Param("undeclared"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown parameter: undeclared", "steps.0.inputs.foo.param"))
  }
}