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

package fr.cnrs.liris.privamov.ops

import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.privamov.core.io._
import fr.cnrs.liris.privamov.core.model.Identified
import fr.cnrs.liris.privamov.core.sparkle.{DataFrame, SparkleEnv}

import scala.reflect._

private[ops] trait SparkleOperator extends SparkleReadOperator with SparkleWriteOperator

private[ops] trait SparkleReadOperator {
  protected def env: SparkleEnv

  protected def decoders: Set[Decoder[_]]

  protected def read[T: ClassTag](dataset: Dataset): DataFrame[T] = {
    val clazz = classTag[T].runtimeClass
    decoders.find(decoder => clazz.isAssignableFrom(decoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No decoder available for: ${clazz.getName}")
      case Some(decoder) => env.read(new CsvSource(dataset.uri, decoder.asInstanceOf[Decoder[T]]))
    }
  }
}

private[ops] trait SparkleWriteOperator {
  protected def encoders: Set[Encoder[_]]

  protected def write[T <: Identified : ClassTag](frame: DataFrame[T], workDir: Path): Dataset =
    write(frame, workDir, "data")

  protected def write[T <: Identified : ClassTag](frame: DataFrame[T], workDir: Path, port: String): Dataset = {
    val clazz = classTag[T].runtimeClass
    encoders.find(encoder => clazz.isAssignableFrom(encoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No encoder available for: ${clazz.getName}")
      case Some(encoder) =>
        val uri = workDir.resolve(port).toAbsolutePath.toString
        frame.write(new CsvSink(uri, encoder.asInstanceOf[Encoder[T]]))
        Dataset(uri)
    }
  }

  protected def write[T: ClassTag](elements: Seq[T], key: String, workDir: Path): Dataset =
    write(elements, key, workDir, "data")

  protected def write[T: ClassTag](elements: Seq[T], key: String, workDir: Path, port: String): Dataset = {
    val clazz = classTag[T].runtimeClass
    encoders.find(encoder => clazz.isAssignableFrom(encoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No encoder available for: ${clazz.getName}")
      case Some(encoder) =>
        val path = workDir.resolve(s"$port/$key.csv").toAbsolutePath
        val textEncoder = new TextLineEncoder(encoder.asInstanceOf[Encoder[T]])
        val bytes = textEncoder.encode(elements)
        Files.createDirectories(path.getParent)
        Files.write(path, bytes)
        Dataset(path.toString)
    }
  }
}