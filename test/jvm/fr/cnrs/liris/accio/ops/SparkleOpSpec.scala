package fr.cnrs.liris.accio.ops

import java.nio.file.Files

import fr.cnrs.liris.accio.core.api.{Dataset, OpContext}
import fr.cnrs.liris.privamov.model.Trace
import fr.cnrs.liris.privamov.sparkle.{CsvSink, CsvSource, SparkleEnv}
import org.scalatest.{BeforeAndAfter, FlatSpec}

trait SparkleOpSpec {
  protected def ctx: OpContext

  protected def env: SparkleEnv

  protected def write(data: Trace*): Dataset = {
    val uri = Files.createTempDirectory("accio-test-").toAbsolutePath.toString
    env.parallelize(data: _*)(_.id).write(CsvSink(uri))
    Dataset(uri, "csv")
  }

  protected def read(ds: Dataset): Seq[Trace] = {
    env.read(CsvSource(ds.uri)).toArray.toSeq
  }
}

trait WithSparkleEnv extends FlatSpec with SparkleOpSpec with BeforeAndAfter {
  protected[this] var env: SparkleEnv = null

  protected[this] def ctx: OpContext = {
    val workDir = Files.createTempDirectory("accio-test-")
    workDir.toFile.deleteOnExit()
    new OpContext(123, workDir)
  }

  before {
    env = new SparkleEnv(1)
  }

  after {
    env.stop()
    env = null
  }
}