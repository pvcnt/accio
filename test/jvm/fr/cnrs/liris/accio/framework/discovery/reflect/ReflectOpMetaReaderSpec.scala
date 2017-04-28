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

package fr.cnrs.liris.accio.framework.discovery.reflect

import fr.cnrs.liris.accio.framework.api.Values
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.discovery.InvalidOpDefException
import fr.cnrs.liris.accio.framework.sdk._
import fr.cnrs.liris.common.geo.{Distance, LatLng, Location}
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[ReflectOpDiscovery]].
 */
class ReflectOpDiscoverySpec extends UnitSpec {
  behavior of "ReflectOpDiscovery"

  private[this] val reader = new ReflectOpDiscovery

  it should "read definition of operators" in {
    val opMeta = reader.read(classOf[DoALotOfThingsOp])
    opMeta.opClass shouldBe classOf[DoALotOfThingsOp]
    opMeta.inClass shouldBe Some(classOf[DoALotOfThingsIn])
    opMeta.outClass shouldBe Some(classOf[DoALotOfThingsOut])

    opMeta.defn.name shouldBe "CustomOpName"
    opMeta.defn.help shouldBe Some("help message")
    opMeta.defn.description shouldBe Some("long description")
    opMeta.defn.category shouldBe "cat"
    opMeta.defn.deprecation shouldBe Some("Deprecated")

    opMeta.defn.inputs should have size 4
    opMeta.defn.inputs(0).name shouldBe "i"
    opMeta.defn.inputs(0).help shouldBe Some("i param")
    opMeta.defn.inputs(1).name shouldBe "s"
    opMeta.defn.inputs(1).help shouldBe None
    opMeta.defn.inputs(2).name shouldBe "foo"
    opMeta.defn.inputs(2).help shouldBe Some("foo input")
    opMeta.defn.inputs(3).name shouldBe "bar"
    opMeta.defn.inputs(3).help shouldBe None

    opMeta.defn.outputs should have size 2
    opMeta.defn.outputs(0).name shouldBe "baz"
    opMeta.defn.outputs(0).help shouldBe Some("baz output")
    opMeta.defn.outputs(1).name shouldBe "bal"
    opMeta.defn.outputs(1).help shouldBe None
  }

  it should "support operators without output" in {
    val meta = reader.read(classOf[NoOutputOp])
    meta.outClass shouldBe None
    meta.defn.outputs should have size 0
  }

  it should "support operators without input" in {
    val meta = reader.read(classOf[NoInputOp])
    meta.inClass shouldBe None
    meta.defn.inputs should have size 0
  }

  it should "have sensitive defaults" in {
    val defn = reader.read(classOf[DefaultOp]).defn
    defn.name shouldBe "Default"
    defn.help shouldBe None
    defn.description shouldBe None
    defn.category shouldBe "misc"
    defn.deprecation shouldBe None
  }

  it should "support byte inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "byte", DataType(AtomicType.Byte))
    assertMandatoryInput(defn, "byte2", DataType(AtomicType.Byte), defaultValue = 2)
    assertOptionalInput(defn, "byte3", DataType(AtomicType.Byte))
  }

  it should "support integer inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "int", DataType(AtomicType.Integer))
    assertMandatoryInput(defn, "int2", DataType(AtomicType.Integer), defaultValue = 2)
    assertOptionalInput(defn, "int3", DataType(AtomicType.Integer))
  }

  it should "support long inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "long", DataType(AtomicType.Long))
    assertMandatoryInput(defn, "long2", DataType(AtomicType.Long), defaultValue = 2)
    assertOptionalInput(defn, "long3", DataType(AtomicType.Long))
  }

  it should "support double inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "dbl", DataType(AtomicType.Double))
    assertMandatoryInput(defn, "dbl2", DataType(AtomicType.Double), defaultValue = 3.14)
    assertOptionalInput(defn, "dbl3", DataType(AtomicType.Double))
  }

  it should "support boolean inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "bool", DataType(AtomicType.Boolean))
    assertMandatoryInput(defn, "bool2", DataType(AtomicType.Boolean), defaultValue = true)
    assertOptionalInput(defn, "bool3", DataType(AtomicType.Boolean))
  }

  it should "support string inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "str", DataType(AtomicType.String))
    assertMandatoryInput(defn, "str2", DataType(AtomicType.String), defaultValue = "some string")
    assertOptionalInput(defn, "str3", DataType(AtomicType.String))
  }

  it should "support location inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "loc", DataType(AtomicType.Location))
    assertMandatoryInput(defn, "loc2", DataType(AtomicType.Location), defaultValue = LatLng.degrees(0, 0))
    assertOptionalInput(defn, "loc3", DataType(AtomicType.Location))
  }

  it should "support timestamp inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "ts", DataType(AtomicType.Timestamp))
    assertMandatoryInput(defn, "ts2", DataType(AtomicType.Timestamp), defaultValue = new Instant(123456))
    assertOptionalInput(defn, "ts3", DataType(AtomicType.Timestamp))
  }

  it should "support duration parameters" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "duration", DataType(AtomicType.Duration))
    assertMandatoryInput(defn, "duration2", DataType(AtomicType.Duration), defaultValue = new Duration(1000))
    assertOptionalInput(defn, "duration3", DataType(AtomicType.Duration))
  }

  it should "support distance parameters" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "dist", DataType(AtomicType.Distance))
    assertMandatoryInput(defn, "dist2", DataType(AtomicType.Distance), defaultValue = Distance.meters(42))
    assertOptionalInput(defn, "dist3", DataType(AtomicType.Distance))
  }

  it should "support list inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "list", DataType(AtomicType.List, Seq(AtomicType.Integer)))
    assertMandatoryInput(defn, "list2", DataType(AtomicType.List, Seq(AtomicType.Integer)), defaultValue = Seq(3, 14))
  }

  it should "support set inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "set", DataType(AtomicType.Set, Seq(AtomicType.Integer)))
    assertMandatoryInput(defn, "set2", DataType(AtomicType.Set, Seq(AtomicType.Integer)), defaultValue = Set(3, 14))
  }

  it should "support map inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "map", DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Integer)))
    assertMandatoryInput(defn, "map2", DataType(AtomicType.Map, Seq(AtomicType.String, AtomicType.Integer)), defaultValue = Map("foo" -> 3, "bar" -> 14))
  }

  it should "support dataset inputs" in {
    val defn = reader.read(classOf[AllDataTypesOp]).defn
    assertMandatoryInput(defn, "data", DataType(AtomicType.Dataset))
    assertMandatoryInput(defn, "data2", DataType(AtomicType.Dataset), defaultValue = Dataset("/dev/null"))
    assertOptionalInput(defn, "data3", DataType(AtomicType.Dataset))
  }

  it should "detect missing @Op annotation" in {
    val expected = intercept[InvalidOpDefException] {
      reader.read(classOf[NonAnnotatedOp])
    }
    expected.getMessage should endWith(": Operator must be annotated with @Op")
  }

  it should "detect missing @Arg annotation" in {
    val expected = intercept[InvalidOpDefException] {
      reader.read(classOf[NonAnnotatedInOp])
    }
    expected.getMessage should endWith(": Input s must be annotated with @Arg")
  }

  it should "support missing @Out annotation" in {
    reader.read(classOf[NonAnnotatedOutOp]).opClass shouldBe classOf[NonAnnotatedOutOp]
  }

  it should "detect unsupported data type" in {
    val expected = intercept[InvalidOpDefException] {
      reader.read(classOf[InvalidParamOp])
    }
    expected.getMessage shouldBe "Illegal definition of operator fr.cnrs.liris.accio.framework.discovery.reflect.InvalidParamOp: Unsupported data type: scala.collection.Iterator"
  }

  it should "detect optional fields to have a default value" in {
    val expected = intercept[InvalidOpDefException] {
      reader.read(classOf[OptionalWithDefaultValueOp])
    }
    expected.getMessage should endWith(": Input i cannot be optional with a default value")
  }

  private def assertMandatoryInput(defn: OpDef, name: String, kind: DataType): Unit =
    doAssertInput(defn, name, kind, optional = false, None)

  private def assertMandatoryInput(defn: OpDef, name: String, kind: DataType, defaultValue: Any): Unit =
    doAssertInput(defn, name, kind, optional = false, Some(defaultValue))

  private def assertOptionalInput(defn: OpDef, name: String, kind: DataType): Unit =
    doAssertInput(defn, name, kind, optional = true, None)

  private def doAssertInput(defn: OpDef, name: String, kind: DataType, optional: Boolean, defaultValue: Option[Any]): Unit = {
    val in = defn.inputs.find(_.name == name).get
    in.kind shouldBe kind
    in.isOptional shouldBe optional
    in.defaultValue shouldBe defaultValue.map(Values.encode(_, kind))
  }
}

case class NoOutputIn(@Arg s: String)

@Op
class NoOutputOp extends Operator[NoOutputIn, Unit] {
  override def execute(in: NoOutputIn, ctx: OpContext): Unit = {}
}

case class NoInputOut(@Arg s: String)

@Op
class NoInputOp extends Operator[Unit, NoInputOut] {
  override def execute(in: Unit, ctx: OpContext): NoInputOut = NoInputOut("foo")
}

private case class AllDataTypesIn(
  @Arg byte: Byte,
  @Arg byte2: Byte = 2,
  @Arg byte3: Option[Byte],
  @Arg int: Int,
  @Arg int2: Int = 2,
  @Arg int3: Option[Int],
  @Arg long: Long,
  @Arg long2: Long = 2,
  @Arg long3: Option[Long],
  @Arg dbl: Double,
  @Arg dbl2: Double = 3.14,
  @Arg dbl3: Option[Double],
  @Arg str: String,
  @Arg str2: String = "some string",
  @Arg str3: Option[String],
  @Arg bool: Boolean,
  @Arg bool2: Boolean = true,
  @Arg bool3: Option[Boolean],
  @Arg loc: Location,
  @Arg loc2: Location = LatLng.degrees(0, 0),
  @Arg loc3: Option[Location],
  @Arg ts: Instant,
  @Arg ts2: Instant = new Instant(123456),
  @Arg ts3: Option[Instant],
  @Arg duration: Duration,
  @Arg duration2: Duration = new Duration(1000),
  @Arg duration3: Option[Duration],
  @Arg dist: Distance,
  @Arg dist2: Distance = Distance.meters(42),
  @Arg dist3: Option[Distance],
  @Arg list: Seq[Int],
  @Arg list2: Seq[Int] = Seq(3, 14),
  @Arg set: Set[Int],
  @Arg set2: Set[Int] = Set(3, 14),
  @Arg map: Map[String, Int],
  @Arg map2: Map[String, Int] = Map("foo" -> 3, "bar" -> 14),
  @Arg data: Dataset,
  @Arg data2: Dataset = Dataset("/dev/null"),
  @Arg data3: Option[Dataset])

@Op
private class AllDataTypesOp extends Operator[AllDataTypesIn, Unit] {
  override def execute(in: AllDataTypesIn, ctx: OpContext): Unit = {}
}

@Op
private class DefaultOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = {}
}

private case class DoALotOfThingsIn(
  @Arg(help = "i param") i: Int,
  @Arg s: String,
  @Arg(help = "foo input") foo: Dataset,
  @Arg bar: Dataset)

private case class DoALotOfThingsOut(@Arg(help = "baz output") baz: Int, @Arg bal: Int)

@Op(
  name = "CustomOpName",
  help = "help message",
  description = "long description",
  category = "cat",
  deprecation = "Deprecated")
private class DoALotOfThingsOp extends Operator[DoALotOfThingsIn, DoALotOfThingsOut] {
  override def execute(in: DoALotOfThingsIn, ctx: OpContext): DoALotOfThingsOut = {
    DoALotOfThingsOut(0, 1)
  }
}

private class NonAnnotatedOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = {}
}

private case class InvalidParamIn(@Arg it: Iterator[Int])

@Op
private class InvalidParamOp extends Operator[InvalidParamIn, Unit] {
  override def execute(in: InvalidParamIn, ctx: OpContext): Unit = {}
}

private case class NonAnnotatedIn(@Arg i: Int, s: String)

@Op
private class NonAnnotatedInOp extends Operator[NonAnnotatedIn, Unit] {
  override def execute(in: NonAnnotatedIn, ctx: OpContext): Unit = {}
}

private case class NonAnnotatedOut(@Arg i: Int, s: String)

@Op
private class NonAnnotatedOutOp extends Operator[Unit, NonAnnotatedOut] {
  override def execute(in: Unit, ctx: OpContext): NonAnnotatedOut = NonAnnotatedOut(0, "foo")
}

private case class OptionalWithDefaultValueIn(@Arg i: Option[Int] = Some(2))

@Op
private class OptionalWithDefaultValueOp extends Operator[OptionalWithDefaultValueIn, Unit] {
  override def execute(in: OptionalWithDefaultValueIn, ctx: OpContext): Unit = {}
}