package fr.cnrs.liris.privamov.ops

import java.nio.file.Files

import fr.cnrs.liris.accio.core.api.{Dataset, OpContext}
import fr.cnrs.liris.privamov.core.io._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv
import org.scalatest.{BeforeAndAfter, FlatSpec}

trait SparkleOpSpec {
  protected def ctx: OpContext

  protected def env: SparkleEnv

  protected def write(data: Trace*): Dataset = {
    val uri = Files.createTempDirectory("accio-test-").toAbsolutePath.toString
    env.parallelize(data: _*)(_.id).write(new CsvSink(uri, new CsvTraceEncoder))
    Dataset(uri)
  }

  protected def read(ds: Dataset): Seq[Trace] = {
    env.read(new CsvSource(ds.uri, new CsvTraceDecoder)).toArray.toSeq
  }
}

trait WithSparkleEnv extends FlatSpec with SparkleOpSpec with BeforeAndAfter {
  protected[this] var env: SparkleEnv = null
  protected[this] val decoders: Set[Decoder[_]] = Set(new CsvTraceDecoder, new CsvPoiSetDecoder)
  protected[this] val encoders: Set[Encoder[_]] = Set(new CsvTraceEncoder, new CsvPoiSetEncoder)

  protected[this] def ctx: OpContext = {
    val workDir = Files.createTempDirectory("accio-test-")
    workDir.toFile.deleteOnExit()
    // This seed make random operators tests to pass for now.
    new OpContext(Some(-7590331047132310476L), workDir)
  }

  before {
    env = new SparkleEnv(1)
  }

  after {
    env.stop()
    env = null
  }
}