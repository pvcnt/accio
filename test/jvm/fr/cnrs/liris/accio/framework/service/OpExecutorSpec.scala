/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.framework.service

import java.nio.file.{Files, Path, Paths}

import fr.cnrs.liris.accio.framework.api.Values
import fr.cnrs.liris.accio.framework.api.thrift._
import fr.cnrs.liris.accio.framework.discovery.reflect.ReflectOpDiscovery
import fr.cnrs.liris.accio.framework.filesystem.FileSystem
import fr.cnrs.liris.accio.framework.sdk._
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.collection.mutable

/**
 * Unit tests for [[OpExecutor]].
 */
class OpExecutorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  private[this] val filesystem = new MockFileSystem
  private[this] var tmpDir: Path = null
  private[this] var executor: OpExecutor = null

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    tmpDir = Files.createTempDirectory("accio-test-")
    executor = new OpExecutor(new ReflectOpDiscovery, filesystem)
    executor.setWorkDir(tmpDir)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    FileUtils.safeDelete(tmpDir)
    tmpDir = null
    executor = null
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    filesystem.clear()
  }

  behavior of "OpExecutor"

  it should "execute operators and return artifacts" in {
    var payload = OpPayload(classOf[SimpleOp].getName, 123, Map("str" -> Values.encodeString("foo")), CacheKey("MyCacheKey"))
    var res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 2
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should contain(Artifact("str", Values.encodeString("foo+0")))
    res.artifacts should contain(Artifact("b", Values.encodeBoolean(false)))

    payload = OpPayload(classOf[SimpleOp].getName, 123, Map("str" -> Values.encodeString("bar"), "i" -> Values.encodeInteger(3)), CacheKey("MyCacheKey"))
    res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 2
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should contain(Artifact("str", Values.encodeString("bar+3")))
    res.artifacts should contain(Artifact("b", Values.encodeBoolean(true)))
  }

  it should "execute operators with no input" in {
    val payload = OpPayload(classOf[NoInputOp].getName, 123, Map.empty, CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 1
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should contain(Artifact("s", Values.encodeString("foo")))
  }

  it should "execute operators with no output" in {
    val payload = OpPayload(classOf[NoOutputOp].getName, 123, Map("s" -> Values.encodeString("foo")), CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 0
    res.exitCode shouldBe 0
    res.error shouldBe None
  }

  it should "detect a missing input" in {
    val payload = OpPayload(classOf[SimpleOp].getName, 123, Map.empty, CacheKey("MyCacheKey"))
    val e = intercept[MissingOpInputException] {
      executor.execute(payload, OpExecutorOpts(useProfiler = false))
    }
    e.op shouldBe "Simple"
    e.arg shouldBe "str"
  }

  it should "detect an unknown operator" in {
    val payload = OpPayload("fr.cnrs.liris.locapriv.UnknownOp", 123, Map.empty, CacheKey("MyCacheKey"))
    val e = intercept[InvalidOpException] {
      executor.execute(payload, OpExecutorOpts(useProfiler = false))
    }
    e.op shouldBe "fr.cnrs.liris.locapriv.UnknownOp"
  }

  it should "catch exceptions thrown by the operator" in {
    val payload = OpPayload(classOf[ExceptionalOp].getName, 123, Map("str" -> Values.encodeString("foo")), CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 0
    res.exitCode shouldNot be(0)
    res.error.isDefined shouldBe true
    res.error.get.root.classifier shouldBe "java.lang.RuntimeException"
    res.error.get.root.message shouldBe Some("Testing exceptions")
  }

  it should "give a seed to unstable operators" in {
    val payload = OpPayload(classOf[UnstableOp].getName, 123, Map.empty, CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 1
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should contain(Artifact("lng", Values.encodeLong(123)))
  }

  it should "not give a seed to non-unstable operators" in {
    val payload = OpPayload(classOf[InvalidUnstableOp].getName, 123, Map.empty, CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 0
    res.exitCode shouldNot be(0)
    res.error.isDefined shouldBe true
    res.error.get.root.classifier shouldBe "java.lang.IllegalStateException"
    res.error.get.root.message shouldBe Some("Operator is not declared as unstable, cannot access the seed")
  }

  it should "upload artifacts" in {
    val payload = OpPayload(classOf[DatasetProducerOp].getName, 123, Map.empty, CacheKey("MyCacheKey"))
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 1
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should have size 1
    res.artifacts.head.name shouldBe "data"
    val dataset = Values.decodeDataset(res.artifacts.head.value)
    dataset.uri should startWith("/mock")
    filesystem.keys should contain(dataset.uri)
  }

  it should "download inputs" in {
    val payload = OpPayload(classOf[DatasetConsumerOp].getName, 123, Map("data" -> Values.encodeDataset(Dataset(s"/mock/data"))), CacheKey("MyCacheKey"))
    filesystem.write(Paths.get("/dev/null"), "data")
    val res = executor.execute(payload, OpExecutorOpts(useProfiler = false))
    res.artifacts should have size 1
    res.exitCode shouldBe 0
    res.error shouldBe None
    res.artifacts should contain(Artifact("ok", Values.encodeBoolean(true)))
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

private class MockFileSystem extends FileSystem {
  private[this] val _keys = mutable.Set.empty[String]

  override def read(src: String, dst: Path): Unit = {
    if (_keys.contains(src)) {
      Files.createFile(dst)
    }
  }

  override def write(src: Path, key: String): String = {
    val uri = s"/mock/$key"
    _keys += uri
    uri
  }

  def keys: Set[String] = _keys.toSet

  def clear(): Unit = _keys.clear()

  override def delete(filename: String): Unit = throw new NotImplementedError
}