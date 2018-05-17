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

import java.nio.file.{Files, Paths}

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.util.geo.{Distance, LatLng, Location}
import org.joda.time.{Duration, Instant}

case class NoOutputOut()

@Op
case class NoOutputOp(@Arg s: String) extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

case class NoInputOut(@Arg s: String)

@Op
case class NoInputOp() extends ScalaOperator[NoInputOut] {
  override def execute(ctx: OpContext): NoInputOut = NoInputOut("foo")
}

@Op
case class AllDataTypesOp(
  @Arg float: Float,
  @Arg float2: Float = 2f,
  @Arg float3: Option[Float],
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
  @Arg ts: Instant,
  @Arg ts2: Instant = new Instant(123456),
  @Arg ts3: Option[Instant],
  @Arg duration: Duration,
  @Arg duration2: Duration = new Duration(1000),
  @Arg duration3: Option[Duration],
  @Arg dist: Distance,
  @Arg dist2: Distance = Distance.meters(42),
  @Arg dist3: Option[Distance],
  @Arg data: RemoteFile,
  @Arg data2: RemoteFile = RemoteFile("/dev/null"),
  @Arg data3: Option[RemoteFile])
  extends ScalaOperator[NoOutputOut] {

  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

@Op
case class DefaultOp() extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

case class DoALotOfThingsOut(@Arg(help = "baz output") baz: Int, @Arg bal: Int)

@Op(
  name = "CustomOpName",
  help = "help message",
  description = "long description",
  category = "cat",
  deprecation = "Deprecated")
case class DoALotOfThingsOp(
  @Arg(help = "i param") i: Int,
  @Arg s: String,
  @Arg(help = "foo input") foo: RemoteFile,
  @Arg bar: RemoteFile)
  extends ScalaOperator[DoALotOfThingsOut] {

  override def execute(ctx: OpContext): DoALotOfThingsOut = DoALotOfThingsOut(0, 1)
}

case class NonAnnotatedOp() extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

@Op
case class InvalidParamOp(@Arg it: Iterator[Int]) extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

@Op
case class NonAnnotatedInOp(@Arg i: Int, s: String) extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

case class NonAnnotatedOut(@Arg i: Int, s: String)

@Op
case class NonAnnotatedOutOp() extends ScalaOperator[NonAnnotatedOut] {
  override def execute(ctx: OpContext): NonAnnotatedOut = NonAnnotatedOut(0, "foo")
}

@Op
case class OptionalWithDefaultValueOp(@Arg i: Option[Int] = Some(2)) extends ScalaOperator[NoOutputOut] {
  override def execute(ctx: OpContext): NoOutputOut = NoOutputOut()
}

case class SimpleOpOut(@Arg str: String, @Arg b: Boolean)

@Op
case class SimpleOp(@Arg str: String, @Arg i: Option[Int]) extends ScalaOperator[SimpleOpOut] {
  override def execute(ctx: OpContext): SimpleOpOut = {
    SimpleOpOut(str + "+" + i.getOrElse(0), i.isDefined)
  }
}

case class UnstableOpOut(@Arg lng: Long)

@Op(unstable = true)
case class UnstableOp() extends ScalaOperator[UnstableOpOut] {
  override def execute(ctx: OpContext): UnstableOpOut = UnstableOpOut(ctx.seed)
}

@Op
case class InvalidUnstableOp() extends ScalaOperator[UnstableOpOut] {
  override def execute(ctx: OpContext): UnstableOpOut = UnstableOpOut(ctx.seed)
}

@Op
case class ExceptionalOp(@Arg str: String, @Arg i: Option[Int]) extends ScalaOperator[SimpleOpOut] {
  override def execute(ctx: OpContext): SimpleOpOut = throw new RuntimeException("Testing exceptions")
}

case class DatasetProducerOut(@Arg data: RemoteFile)

@Op
case class DatasetProducerOp() extends ScalaOperator[DatasetProducerOut] {
  override def execute(ctx: OpContext): DatasetProducerOut = {
    val dir = ctx.workDir.resolve("data")
    Files.createDirectory(dir)
    DatasetProducerOut(RemoteFile(dir.toAbsolutePath.toString))
  }
}

case class DatasetConsumerOut(@Arg ok: Boolean)

@Op
case class DatasetConsumerOp(@Arg data: RemoteFile) extends ScalaOperator[DatasetConsumerOut] {
  override def execute(ctx: OpContext): DatasetConsumerOut = {
    val ok = Paths.get(data.uri).toFile.exists
    DatasetConsumerOut(ok)
  }
}