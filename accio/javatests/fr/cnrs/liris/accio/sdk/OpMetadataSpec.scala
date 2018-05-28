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

package fr.cnrs.liris.accio.sdk

import fr.cnrs.liris.accio.domain.{DataTypes, Operator}
import fr.cnrs.liris.lumos.domain.{DataType, RemoteFile, Value}
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[OpMetadata]].
 */
class OpMetadataSpec extends UnitSpec {
  behavior of "OpMetadata"

  DataTypes.register()

  it should "read definition of operators" in {
    val opMeta = OpMetadata.apply[DoALotOfThingsOp]
    opMeta.clazz shouldBe classOf[DoALotOfThingsOp]

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
    val opMeta = OpMetadata.apply[NoOutputOp]
    opMeta.defn.outputs should have size 0
  }

  it should "support operators without input" in {
    val opMeta = OpMetadata.apply[NoInputOp]
    opMeta.defn.inputs should have size 0
  }

  it should "have sensitive defaults" in {
    val defn = OpMetadata.apply[DefaultOp].defn
    defn.name shouldBe "Default"
    defn.help shouldBe None
    defn.description shouldBe None
    defn.category shouldBe "misc"
    defn.deprecation shouldBe None
  }

  it should "support float inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "float", DataType.Float)
    assertMandatoryInput(defn, "float2", DataType.Float, defaultValue = 2f)
    assertOptionalInput(defn, "float3", DataType.Float)
  }

  it should "support integer inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "int", DataType.Int)
    assertMandatoryInput(defn, "int2", DataType.Int, defaultValue = 2)
    assertOptionalInput(defn, "int3", DataType.Int)
  }

  it should "support long inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "long", DataType.Long)
    assertMandatoryInput(defn, "long2", DataType.Long, defaultValue = 2L)
    assertOptionalInput(defn, "long3", DataType.Long)
  }

  it should "support double inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "dbl", DataType.Double)
    assertMandatoryInput(defn, "dbl2", DataType.Double, defaultValue = 3.14)
    assertOptionalInput(defn, "dbl3", DataType.Double)
  }

  it should "support boolean inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "bool", DataType.Bool)
    assertMandatoryInput(defn, "bool2", DataType.Bool, defaultValue = true)
    assertOptionalInput(defn, "bool3", DataType.Bool)
  }

  it should "support string inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "str", DataType.String)
    assertMandatoryInput(defn, "str2", DataType.String, defaultValue = "some string")
    assertOptionalInput(defn, "str3", DataType.String)
  }

  it should "support timestamp inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "ts", DataTypes.Timestamp)
    assertMandatoryInput(defn, "ts2", DataTypes.Timestamp, defaultValue = new Instant(123456))
    assertOptionalInput(defn, "ts3", DataTypes.Timestamp)
  }

  it should "support duration parameters" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "duration", DataTypes.Duration)
    assertMandatoryInput(defn, "duration2", DataTypes.Duration, defaultValue = new Duration(1000))
    assertOptionalInput(defn, "duration3", DataTypes.Duration)
  }

  it should "support distance parameters" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "dist", DataTypes.Distance)
    assertMandatoryInput(defn, "dist2", DataTypes.Distance, defaultValue = Distance.meters(42))
    assertOptionalInput(defn, "dist3", DataTypes.Distance)
  }

  it should "support dataset inputs" in {
    val defn = OpMetadata.apply[AllDataTypesOp].defn
    assertMandatoryInput(defn, "data", DataType.File)
    assertMandatoryInput(defn, "data2", DataType.File, defaultValue = RemoteFile("/dev/null"))
    assertOptionalInput(defn, "data3", DataType.File)
  }

  it should "detect missing @Op annotation" in {
    val expected = intercept[IllegalArgumentException] {
      OpMetadata.apply[NonAnnotatedOp]
    }
    expected.getMessage shouldBe "Operator in fr.cnrs.liris.accio.sdk.NonAnnotatedOp must be annotated with @Op"
  }

  it should "detect missing @Arg annotation" in {
    val expected = intercept[IllegalArgumentException] {
      OpMetadata.apply[NonAnnotatedInOp]
    }
    expected.getMessage shouldBe "Input fr.cnrs.liris.accio.sdk.NonAnnotatedInOp.s must be annotated with @Arg"
  }

  it should "support missing @Arg annotation on output" in {
    OpMetadata.apply[NonAnnotatedOutOp].clazz shouldBe classOf[NonAnnotatedOutOp]
  }

  it should "detect unsupported data type" in {
    val expected = intercept[IllegalArgumentException] {
      OpMetadata.apply[InvalidParamOp]
    }
    expected.getMessage shouldBe "Unsupported Scala type: scala.collection.Iterator"
  }

  it should "detect optional fields to have a default value" in {
    val expected = intercept[IllegalArgumentException] {
      OpMetadata.apply[OptionalWithDefaultValueOp]
    }
    expected.getMessage shouldBe "Input fr.cnrs.liris.accio.sdk.OptionalWithDefaultValueOp.i cannot be optional with a default value"
  }

  private def assertMandatoryInput(defn: Operator, name: String, dataType: DataType, defaultValue: Any = null): Unit =
    doAssertInput(defn, name, dataType, optional = false, defaultValue = Option(defaultValue))

  private def assertOptionalInput(defn: Operator, name: String, dataType: DataType): Unit =
    doAssertInput(defn, name, dataType, optional = true, defaultValue = None)

  private def doAssertInput(defn: Operator, name: String, dataType: DataType, optional: Boolean, defaultValue: Option[Any]): Unit = {
    val in = defn.inputs.find(_.name == name).get
    in.dataType shouldBe dataType
    in.optional shouldBe optional
    in.defaultValue shouldBe defaultValue.map(Value.apply(_, dataType))
  }
}