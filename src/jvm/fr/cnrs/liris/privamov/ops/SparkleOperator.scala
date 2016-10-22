package fr.cnrs.liris.privamov.ops

import java.nio.file.Path

import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.privamov.core.sparkle.{DataFrame, SparkleEnv}
import fr.cnrs.liris.privamov.core.sparkle.{CsvSink, CsvSource}
import fr.cnrs.liris.privamov.core.model.Trace

private[ops] trait SparkleOperator {

  protected def read(dataset: Dataset, env: SparkleEnv) = {
    require(dataset.format == "csv", s"Only CSV datasets are supported, got: ${dataset.format}")
    env.read(CsvSource(dataset.uri))
  }

  protected def write(frame: DataFrame[Trace], workDir: Path, port: String = "data") = {
    val uri = workDir.resolve(port).toAbsolutePath.toString
    frame.write(CsvSink(uri))
    Dataset(uri, format = "csv")
  }
}