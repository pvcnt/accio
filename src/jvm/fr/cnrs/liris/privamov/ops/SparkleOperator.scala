/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.ops

import java.nio.file.Path

import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.privamov.core.io.{CsvSink, CsvSource}
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.{DataFrame, SparkleEnv}

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