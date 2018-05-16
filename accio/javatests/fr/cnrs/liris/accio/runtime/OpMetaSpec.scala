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

package fr.cnrs.liris.accio.runtime

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._
import fr.cnrs.liris.accio.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.{Distance, LatLng}
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[OpMeta]].
 */
class OpMetaSpec extends UnitSpec {
  behavior of "OpMeta"

  it should "read definition of operators" in {
    val opMeta = OpMeta.apply[DoALotOfThingsOp]
    opMeta.opClass.runtimeClass shouldBe classOf[DoALotOfThingsOp]

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
    val meta = OpMeta.apply[NoOutputOp]
    meta.defn.outputs should have size 0
  }

  it should "support operators without input" in {
    val meta = OpMeta.apply[NoInputOp]
    meta.defn.inputs should have size 0
  }

  it should "have sensitive defaults" in {
    val defn = OpMeta.apply[DefaultOp].defn
    defn.name shouldBe "Default"
    defn.help shouldBe None
    defn.description shouldBe None
    defn.category shouldBe "misc"
    defn.deprecation shouldBe None
  }

  it should "support float inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "float", DataType.Atomic(AtomicType.Float))
    assertMandatoryInput(defn, "float2", DataType.Atomic(AtomicType.Float), defaultValue = 2f)
    assertOptionalInput(defn, "float3", DataType.Atomic(AtomicType.Float))
  }

  it should "support integer inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "int", DataType.Atomic(AtomicType.Integer))
    assertMandatoryInput(defn, "int2", DataType.Atomic(AtomicType.Integer), defaultValue = 2)
    assertOptionalInput(defn, "int3", DataType.Atomic(AtomicType.Integer))
  }

  it should "support long inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "long", DataType.Atomic(AtomicType.Long))
    assertMandatoryInput(defn, "long2", DataType.Atomic(AtomicType.Long), defaultValue = 2)
    assertOptionalInput(defn, "long3", DataType.Atomic(AtomicType.Long))
  }

  it should "support double inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "dbl", DataType.Atomic(AtomicType.Double))
    assertMandatoryInput(defn, "dbl2", DataType.Atomic(AtomicType.Double), defaultValue = 3.14)
    assertOptionalInput(defn, "dbl3", DataType.Atomic(AtomicType.Double))
  }

  it should "support boolean inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "bool", DataType.Atomic(AtomicType.Boolean))
    assertMandatoryInput(defn, "bool2", DataType.Atomic(AtomicType.Boolean), defaultValue = true)
    assertOptionalInput(defn, "bool3", DataType.Atomic(AtomicType.Boolean))
  }

  it should "support string inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "str", DataType.Atomic(AtomicType.String))
    assertMandatoryInput(defn, "str2", DataType.Atomic(AtomicType.String), defaultValue = "some string")
    assertOptionalInput(defn, "str3", DataType.Atomic(AtomicType.String))
  }

  it should "support location inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "loc", DataType.Atomic(AtomicType.Location))
    assertMandatoryInput(defn, "loc2", DataType.Atomic(AtomicType.Location), defaultValue = LatLng.degrees(0, 0))
    assertOptionalInput(defn, "loc3", DataType.Atomic(AtomicType.Location))
  }

  it should "support timestamp inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "ts", DataType.Atomic(AtomicType.Timestamp))
    assertMandatoryInput(defn, "ts2", DataType.Atomic(AtomicType.Timestamp), defaultValue = new Instant(123456))
    assertOptionalInput(defn, "ts3", DataType.Atomic(AtomicType.Timestamp))
  }

  it should "support duration parameters" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "duration", DataType.Atomic(AtomicType.Duration))
    assertMandatoryInput(defn, "duration2", DataType.Atomic(AtomicType.Duration), defaultValue = new Duration(1000))
    assertOptionalInput(defn, "duration3", DataType.Atomic(AtomicType.Duration))
  }

  it should "support distance parameters" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "dist", DataType.Atomic(AtomicType.Distance))
    assertMandatoryInput(defn, "dist2", DataType.Atomic(AtomicType.Distance), defaultValue = Distance.meters(42))
    assertOptionalInput(defn, "dist3", DataType.Atomic(AtomicType.Distance))
  }

  it should "support list inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "list", DataType.ListType(ListType(AtomicType.Integer)))
    assertMandatoryInput(defn, "list2", DataType.ListType(ListType(AtomicType.Integer)), defaultValue = Seq(3, 14))
  }

  it should "support map inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "map", DataType.MapType(MapType(AtomicType.String, AtomicType.Integer)))
    assertMandatoryInput(defn, "map2", DataType.MapType(MapType(AtomicType.String, AtomicType.Integer)), defaultValue = Map("foo" -> 3, "bar" -> 14))
  }

  it should "support dataset inputs" in {
    val defn = OpMeta.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "data", DataType.Dataset(DatasetType()))
    assertMandatoryInput(defn, "data2", DataType.Dataset(DatasetType()), defaultValue = RemoteFile("/dev/null"))
    assertOptionalInput(defn, "data3", DataType.Dataset(DatasetType()))
  }

  it should "detect missing @Op annotation" in {
    val expected = intercept[IllegalArgumentException] {
      OpMeta.apply[NonAnnotatedOp]
    }
    expected.getMessage shouldBe "Operator in fr.cnrs.liris.accio.runtime.NonAnnotatedOp must be annotated with @Op"
  }

  it should "detect missing @Arg annotation" in {
    val expected = intercept[IllegalArgumentException] {
      OpMeta.apply[NonAnnotatedInOp]
    }
    expected.getMessage shouldBe "Input fr.cnrs.liris.accio.runtime.NonAnnotatedInOp.s must be annotated with @Arg"
  }

  it should "support missing @Arg annotation on output" in {
    OpMeta.apply[NonAnnotatedOutOp].opClass.runtimeClass shouldBe classOf[NonAnnotatedOutOp]
  }

  it should "detect unsupported data type" in {
    val expected = intercept[IllegalArgumentException] {
      OpMeta.apply[InvalidParamOp]
    }
    expected.getMessage shouldBe "Unsupported data type: Iterator[Int]"
  }

  it should "detect optional fields to have a default value" in {
    val expected = intercept[IllegalArgumentException] {
      OpMeta.apply[OptionalWithDefaultValueOp]
    }
    expected.getMessage shouldBe "Input fr.cnrs.liris.accio.runtime.OptionalWithDefaultValueOp.i cannot be optional with a default value"
  }

  private def assertMandatoryInput(defn: Operator, name: String, dataType: DataType): Unit =
    doAssertInput(defn, name, dataType, optional = false, None)

  private def assertMandatoryInput(defn: Operator, name: String, dataType: DataType, defaultValue: Any): Unit =
    doAssertInput(defn, name, dataType, optional = false, Some(defaultValue))

  private def assertOptionalInput(defn: Operator, name: String, dataType: DataType): Unit =
    doAssertInput(defn, name, dataType, optional = true, None)

  private def doAssertInput(defn: Operator, name: String, dataType: DataType, optional: Boolean, defaultValue: Option[Any]): Unit = {
    val in = defn.inputs.find(_.name == name).get
    in.dataType shouldBe dataType
    in.isOptional shouldBe optional
    in.defaultValue shouldBe defaultValue.map(Values.encode(_, dataType).get)
  }
}