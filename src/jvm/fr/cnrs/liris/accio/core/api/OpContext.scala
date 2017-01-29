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

package fr.cnrs.liris.accio.core.api

import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.core.api.io._
import fr.cnrs.liris.accio.core.api.sparkle._
import fr.cnrs.liris.common.util.Identified

import scala.reflect.{ClassTag, classTag}

/**
 * Execution context of an operator.
 *
 * @param _seed    Seed used by an unstable operator, if it is the case.
 * @param workDir  Working directory where temporary data can be written. It will be removed after operator completion.
 * @param env      Sparkle environment.
 * @param decoders Decoders available to read data from CSV files.
 * @param encoders Encoders available to write data to CSV files.
 */
class OpContext(_seed: Option[Long], val workDir: Path, val env: SparkleEnv, decoders: Set[Decoder[_]], encoders: Set[Encoder[_]]) {
  /**
   * Return the seed to use for an unstable operator.
   *
   * @throws IllegalStateException If the operator is not declared as unstable.
   */
  @throws[IllegalStateException]
  def seed: Long = _seed match {
    case None => throw new IllegalStateException("Operator is not declared as unstable, cannot access the seed")
    case Some(s) => s
  }

  def read[T: ClassTag](dataset: Dataset): DataFrame[T] = {
    val clazz = classTag[T].runtimeClass
    decoders.find(decoder => clazz.isAssignableFrom(decoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No decoder available for ${clazz.getName}")
      case Some(decoder) => env.read(new CsvSource(dataset.uri, decoder.asInstanceOf[Decoder[T]]))
    }
  }

  def write[T <: Identified : ClassTag](frame: DataFrame[T]): Dataset = write(frame, "data")

  def write[T <: Identified : ClassTag](frame: DataFrame[T], port: String): Dataset = {
    val clazz = classTag[T].runtimeClass
    encoders.find(encoder => clazz.isAssignableFrom(encoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No encoder available for ${clazz.getName}")
      case Some(encoder) =>
        val uri = workDir.resolve(port).toAbsolutePath.toString
        frame.write(new CsvSink(uri, encoder.asInstanceOf[Encoder[T]]))
        Dataset(uri)
    }
  }

  def write[T: ClassTag](elements: Seq[T], key: String): Dataset = write(elements, key, "data")

  def write[T: ClassTag](elements: Seq[T], key: String, port: String): Dataset = {
    val clazz = classTag[T].runtimeClass
    encoders.find(encoder => clazz.isAssignableFrom(encoder.elementClassTag.runtimeClass)) match {
      case None => throw new RuntimeException(s"No encoder available for ${clazz.getName}")
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