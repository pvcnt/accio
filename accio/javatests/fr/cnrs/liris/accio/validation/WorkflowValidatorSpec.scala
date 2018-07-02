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

package fr.cnrs.liris.accio.validation

import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain._
import fr.cnrs.liris.accio.testing.MemoryOpDiscovery
import fr.cnrs.liris.accio.validation.ValidationResult.FieldViolation
import fr.cnrs.liris.lumos.domain.{AttrValue, DataType, RemoteFile, Value}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[WorkflowValidator]].
 */
class WorkflowValidatorSpec extends UnitSpec {
  behavior of "WorkflowValidator"

  private object FixedNamedGenerator extends NameGenerator {
    override def generateName(): String = "non-random-name"
  }

  private val validator = {
    val registry = new OpRegistry(new MemoryOpDiscovery(Set(Operator(
      name = "FirstSimple",
      executable = RemoteFile("."),
      inputs = Seq(Attribute("foo", DataType.Int)),
      outputs = Seq(Attribute("data", DataType.Dataset))),
      Operator(
        name = "SecondSimple",
        executable = RemoteFile("."),
        inputs = Seq(
          Attribute("dbl", DataType.Double),
          Attribute("str", DataType.String, defaultValue = Some(Value.String("something"))),
          Attribute("data", DataType.Dataset)),
        outputs = Seq(Attribute("data", DataType.Dataset))),
      Operator(
        name = "ThirdSimple",
        executable = RemoteFile("."),
        inputs = Seq(
          Attribute("data1", DataType.Dataset),
          Attribute("data2", DataType.Dataset)),
        outputs = Seq(Attribute("data", DataType.Dataset))),
      Operator(
        name = "Deprecated",
        executable = RemoteFile("."),
        deprecation = Some("Do not use it!"),
        inputs = Seq(Attribute("foo", DataType.Int)),
        outputs = Seq(Attribute("data", DataType.Dataset))))))
    new WorkflowValidator(registry, FixedNamedGenerator)
  }

  it should "overwrite the owner if specified" in {
    var obj = Workflow(name = "some-job")
    var workflow = validator.prepare(obj, Some("me"))
    workflow.owner shouldBe Some("me")

    obj = Workflow(name = "some-job", owner = Some("him"))
    workflow = validator.prepare(obj, Some("me"))
    workflow.owner shouldBe Some("me")
  }

  it should "generate a seed if empty" in {
    var obj = Workflow(name = "some-job")
    var workflow = validator.prepare(obj, None)
    workflow.seed shouldNot be(0L)

    // ... but it should not overwrite an already-defined seed.
    obj = Workflow(name = "some-job", seed = 1234L)
    workflow = validator.prepare(obj, None)
    workflow.seed shouldBe 1234L
  }

  it should "generate a name if empty" in {
    var obj = Workflow()
    var workflow = validator.prepare(obj, None)
    workflow.name shouldBe "non-random-name"

    // ... but it should not overwrite an already-defined name.
    obj = Workflow(name = "some-job")
    workflow = validator.prepare(obj, None)
    workflow.name shouldBe "some-job"
  }

  it should "fill step name if empty" in {
    var obj = Workflow(
      name = "some-job",
      steps = Seq(Step(
        op = "FirstSimple",
        params = Seq(Channel("foo", Channel.Constant(Value.Int(42)))))))
    var workflow = validator.prepare(obj, None)
    workflow.steps.head.name shouldBe "FirstSimple"

    // ... but it should not overwrite an already-defined name.
    obj = Workflow(
      name = "some-job",
      steps = Seq(Step(
        op = "FirstSimple",
        name = "UserDefinedName",
        params = Seq(Channel("foo", Channel.Constant(Value.Int(42)))))))
    workflow = validator.prepare(obj, None)
    workflow.steps.head.name shouldBe "UserDefinedName"
  }

  it should "fill step input name if empty" in {
    val obj = Workflow(
      name = "some-job",
      steps = Seq(Step(
        op = "SecondSimple",
        params = Seq(
          Channel("dbl", Channel.Constant(Value.Double(3.14))),
          Channel("", Channel.Constant(Value.String("bar")))))))
    val workflow = validator.prepare(obj, None)
    workflow.steps.head.params shouldBe Seq(
      Channel("dbl", Channel.Constant(Value.Double(3.14))),
      Channel("str", Channel.Constant(Value.String("bar"))))
  }

  it should "cast params to the most appropriate type" in {
    var obj = Workflow(
      name = "some-job",
      params = Seq(AttrValue("param", Value.String("42"))),
      steps = Seq(Step(
        op = "FirstSimple",
        params = Seq(Channel("foo", Channel.Param("param"))))))
    var workflow = validator.prepare(obj, None)
    workflow.params shouldBe Seq(AttrValue("param", Value.Int(42)))

    obj = Workflow(
      name = "some-job",
      params = Seq(AttrValue("param", Value.String("42"))),
      steps = Seq(Step(
        op = "FirstSimple",
        params = Seq(Channel("foo", Channel.Param("param")))),
        Step(
          op = "SecondSimple",
          params = Seq(Channel("i", Channel.Param("param"))))))
    workflow = validator.prepare(obj, None)
    workflow.params shouldBe Seq(AttrValue("param", Value.Int(42)))
  }

  it should "validate a legitimate wofklow" in {
    val obj = Workflow(
      name = "valid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "FirstSimple",
          name = "FirstSimple1",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "ThirdSimple",
          name = "ThirdSimple",
          params = Seq(
            Channel("data1", Channel.Reference("FirstSimple", "data")),
            Channel("data2", Channel.Reference("FirstSimple1", "data")))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("FirstSimple", "data"))))))
    val res = validator.validate(obj)
    res.errors should have size 0
    res.warnings should have size 0
    res.isValid shouldBe true
  }

  it should "reject a duplicate step name" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "SecondSimple",
          name = "FirstSimple",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("FirstSimple", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain(FieldViolation("Duplicate step name: FirstSimple (appears 2 times)", "steps.0.name"))
    res.errors should contain(FieldViolation("Duplicate step name: FirstSimple (appears 2 times)", "steps.1.name"))
  }

  it should "reject an unknown operator" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(Step(op = "InvalidOp", name = "MyOp")))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Unknown operator: InvalidOp", "steps.0.op"))
  }

  it should "detect an unknown input name" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(
            Channel("foo", Channel.Constant(Value.Int(42))),
            Channel("bar", Channel.Constant(Value.Int(43)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe true
    res.warnings should contain theSameElementsAs Set(
      FieldViolation("Unknown parameter for operator FirstSimple: bar", "steps.0.inputs.bar"))
  }

  it should "reject an invalid operator param type" in {
    var obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.String("42.4")))))))
    var res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Data type mismatch: requires Int, got String", "steps.0.inputs.foo.constant"))

    obj = Workflow(
      name = "valid-job",
      params = Seq(AttrValue("p", Value.String("42.4"))),
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Param("p"))))))
    res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("Data type mismatch: requires Int, got String", "steps.0.inputs.foo.param"))
  }

  it should "reject an unknown reference step name" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("UnknownTesting", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Unknown step: UnknownTesting", "steps.1.inputs.data.step"))
  }

  it should "reject an unknown reference port name" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("FirstSimple", "unknown"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Unknown output for operator FirstSimple: FirstSimple/unknown", "steps.1.inputs.data.port"))
  }

  it should "reject a job without any roots" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "SecondSimple",
          name = "First",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("Second", "data")))),
        Step(
          op = "SecondSimple",
          name = "Second",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("First", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(FieldViolation("No root step", "steps"))
  }

  it should "reject inconsistent data types in references" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "First",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "FirstSimple",
          name = "Second",
          params = Seq(Channel("foo", Channel.Reference("First", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Data type mismatch: requires Int, got Dataset", "steps.1.inputs.foo"))
  }

  it should "reject a missing parameter" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(Step(op = "FirstSimple", name = "FirstSimple")))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Required parameter for operator FirstSimple is missing: foo", "steps.0.inputs"))
  }

  it should "reject an invalid step name" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "First/Simple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Illegal value: First/Simple (should match [a-zA-Z0-9][a-zA-Z0-9._-]*)", "steps.0.name"))
  }

  it should "reject a cyclic graph" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42))))),
        Step(
          op = "ThirdSimple",
          name = "ThirdSimple",
          params = Seq(
            Channel("data1", Channel.Reference("FirstSimple", "data")),
            Channel("data2", Channel.Reference("SecondSimple", "data")))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          params = Seq(
            Channel("dbl", Channel.Constant(Value.Double(3.14))),
            Channel("data", Channel.Reference("ThirdSimple", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Cycle detected: ThirdSimple -> SecondSimple -> ThirdSimple", "steps"))
  }

  it should "detect a deprecated operator" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "Deprecated",
          name = "Deprecated",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(42)))))))
    val res = validator.validate(obj)
    res.warnings should contain(FieldViolation("Operator Deprecated is deprecated: Do not use it!", "steps.0.op"))
  }

  it should "detect an invalid job name" in {
    val obj = Workflow(
      name = "workflow!id",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.Int(2)))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Illegal value: workflow!id (should match [a-zA-Z0-9][a-zA-Z0-9._-]*)", "name"))
  }

  it should "reject a param type mismatch" in {
    val obj = Workflow(
      name = "invalid-job",
      params = Seq(AttrValue("foo", Value.False)),
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Param("foo")))),
        Step(
          op = "SecondSimple",
          name = "SecondSimple",
          params = Seq(
            Channel("dbl", Channel.Param("foo")),
            Channel("data", Channel.Reference("FirstSimple", "data"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Data type mismatch: requires Int, got Bool", "steps.0.inputs.foo.param"),
      FieldViolation("Data type mismatch: requires Double, got Bool", "steps.1.inputs.dbl.param"))
  }

  it should "reject an invalid param name" in {
    val obj = Workflow(
      name = "invalid-job",
      params = Seq(AttrValue("foo/foo", Value.Int(1))),
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Param("foo/foo"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Illegal value: foo/foo (should match [a-zA-Z0-9][a-zA-Z0-9._-]*)", "params.0.name"))
  }

  it should "reject an undeclared param" in {
    val obj = Workflow(
      name = "invalid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Param("undeclared"))))))
    val res = validator.validate(obj)
    res.isValid shouldBe false
    res.errors should contain theSameElementsAs Set(
      FieldViolation("Unknown parameter: undeclared", "steps.0.inputs.foo.param"))
  }

  it should "support casting parameters" in {
    var obj = Workflow(
      name = "valid-job",
      params = Seq(AttrValue("p", Value.String("42"))),
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Param("p"))))))
    var res = validator.validate(obj)
    res.errors should have size 0
    res.warnings should have size 0
    res.isValid shouldBe true

    obj = Workflow(
      name = "valid-job",
      steps = Seq(
        Step(
          op = "FirstSimple",
          name = "FirstSimple",
          params = Seq(Channel("foo", Channel.Constant(Value.String("42")))))))
    res = validator.validate(obj)
    res.errors should have size 0
    res.warnings should have size 0
    res.isValid shouldBe true
  }
}