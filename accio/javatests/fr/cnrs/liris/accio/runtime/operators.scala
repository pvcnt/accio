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

import java.nio.file.{Files, Paths}

import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.util.geo.{Distance, LatLng, Location}
import org.joda.time.{Duration, Instant}

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

case class AllDataTypesIn(
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
class AllDataTypesOp extends Operator[AllDataTypesIn, Unit] {
  override def execute(in: AllDataTypesIn, ctx: OpContext): Unit = {}
}

@Op
class DefaultOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = {}
}

case class DoALotOfThingsIn(
  @Arg(help = "i param") i: Int,
  @Arg s: String,
  @Arg(help = "foo input") foo: Dataset,
  @Arg bar: Dataset)

case class DoALotOfThingsOut(@Arg(help = "baz output") baz: Int, @Arg bal: Int)

@Op(
  name = "CustomOpName",
  help = "help message",
  description = "long description",
  category = "cat",
  deprecation = "Deprecated")
class DoALotOfThingsOp extends Operator[DoALotOfThingsIn, DoALotOfThingsOut] {
  override def execute(in: DoALotOfThingsIn, ctx: OpContext): DoALotOfThingsOut = {
    DoALotOfThingsOut(0, 1)
  }
}

class NonAnnotatedOp extends Operator[Unit, Unit] {
  override def execute(in: Unit, ctx: OpContext): Unit = {}
}

case class InvalidParamIn(@Arg it: Iterator[Int])

@Op
class InvalidParamOp extends Operator[InvalidParamIn, Unit] {
  override def execute(in: InvalidParamIn, ctx: OpContext): Unit = {}
}

case class NonAnnotatedIn(@Arg i: Int, s: String)

@Op
class NonAnnotatedInOp extends Operator[NonAnnotatedIn, Unit] {
  override def execute(in: NonAnnotatedIn, ctx: OpContext): Unit = {}
}

case class NonAnnotatedOut(@Arg i: Int, s: String)

@Op
class NonAnnotatedOutOp extends Operator[Unit, NonAnnotatedOut] {
  override def execute(in: Unit, ctx: OpContext): NonAnnotatedOut = NonAnnotatedOut(0, "foo")
}

case class OptionalWithDefaultValueIn(@Arg i: Option[Int] = Some(2))

@Op
class OptionalWithDefaultValueOp extends Operator[OptionalWithDefaultValueIn, Unit] {
  override def execute(in: OptionalWithDefaultValueIn, ctx: OpContext): Unit = {}
}

case class SimpleOpIn(@Arg str: String, @Arg i: Option[Int])

case class SimpleOpOut(@Arg str: String, @Arg b: Boolean)

@Op
class SimpleOp extends Operator[SimpleOpIn, SimpleOpOut] {
  override def execute(in: SimpleOpIn, ctx: OpContext): SimpleOpOut = {
    SimpleOpOut(in.str + "+" + in.i.getOrElse(0), in.i.isDefined)
  }
}

case class UnstableOpIn()

case class UnstableOpOut(@Arg lng: Long)

@Op(unstable = true)
class UnstableOp extends Operator[UnstableOpIn, UnstableOpOut] {
  override def execute(in: UnstableOpIn, ctx: OpContext): UnstableOpOut = {
    UnstableOpOut(ctx.seed)
  }
}

@Op
class InvalidUnstableOp extends Operator[UnstableOpIn, UnstableOpOut] {
  override def execute(in: UnstableOpIn, ctx: OpContext): UnstableOpOut = {
    UnstableOpOut(ctx.seed)
  }
}

@Op
class ExceptionalOp extends Operator[SimpleOpIn, SimpleOpOut] {
  override def execute(in: SimpleOpIn, ctx: OpContext): SimpleOpOut = {
    throw new RuntimeException("Testing exceptions")
  }
}

case class DatasetProducerOut(@Arg data: Dataset)

@Op
class DatasetProducerOp extends Operator[Unit, DatasetProducerOut] {
  override def execute(in: Unit, ctx: OpContext): DatasetProducerOut = {
    val dir = ctx.workDir.resolve("data")
    Files.createDirectory(dir)
    DatasetProducerOut(Dataset(dir.toAbsolutePath.toString))
  }
}

case class DatasetConsumerIn(@Arg data: Dataset)

case class DatasetConsumerOut(@Arg ok: Boolean)

@Op
class DatasetConsumerOp extends Operator[DatasetConsumerIn, DatasetConsumerOut] {
  override def execute(in: DatasetConsumerIn, ctx: OpContext): DatasetConsumerOut = {
    val ok = Paths.get(in.data.uri).toFile.exists
    DatasetConsumerOut(ok)
  }
}