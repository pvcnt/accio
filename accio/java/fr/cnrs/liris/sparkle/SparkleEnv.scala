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

package fr.cnrs.liris.sparkle

import java.util.concurrent.{Executors, TimeUnit}

import com.twitter.util.logging.Logging
import com.twitter.util.{Await, Future, FuturePool}

import scala.reflect.ClassTag

/**
 * A Sparkle environment is responsible for the execution of parallel tasks on [[DataFrame]]s.
 *
 * @param parallelism Parallelism level (i.e., number of cores to use).
 */
final class SparkleEnv(parallelism: Int) extends Logging {
  require(parallelism > 0, s"parallelism must be strictly positive: $parallelism")
  private[this] val pool = {
    val executor = if (1 == parallelism) Executors.newSingleThreadExecutor else Executors.newWorkStealingPool(parallelism)
    FuturePool(executor)
  }
  logger.info(s"Initialized Sparkle with $parallelism cores")

  /**
   * Create a new dataframe from an in-memory collection.
   *
   * @param first First element.
   * @param rest  Other elements.
   * @tparam T Elements' type.
   */
  def parallelize[T: Encoder](first: T, rest: T*): DataFrame[T] = parallelize(first +: rest)

  /**
   * Create a new dataframe from an in-memory collection.
   *
   * @param data Elements.
   * @tparam T Elements' type.
   */
  def parallelize[T: Encoder](data: Iterable[T]): DataFrame[T] = {
    if (data.isEmpty) {
      emptyDataFrame
    } else {
      new MemoryDataFrame(data.toSeq, parallelism, this, implicitly[Encoder[T]])
    }
  }

  /**
   * Create an empty dataframe.
   *
   * @tparam T Elements' type.
   */
  def emptyDataFrame[T: Encoder]: DataFrame[T] = new EmptyDataFrame(this, implicitly[Encoder[T]])

  /**
   * Create a new dataframe by reading data stored elsewhere.
   *
   * @tparam T Elements' type.
   * @return A dataframe reader.
   */
  def read[T: Encoder]: DataFrameReader[T] = new DataFrameReader(this, implicitly[Encoder[T]])

  /**
   * Clean and stop this environment. It will not be usable after. This method blocks until the
   * environment is effectively stopped.
   */
  def stop(): Unit = synchronized {
    if (!pool.executor.isTerminated) {
      pool.executor.shutdown()
      while (!pool.executor.isTerminated) {
        pool.executor.awaitTermination(100, TimeUnit.MILLISECONDS)
      }
    }
  }

  /**
   * Submit a job to this environment.
   *
   * @param df
   * @param keys
   * @param processor
   * @tparam T
   * @tparam U
   */
  private[sparkle] def submit[T, U: ClassTag](df: DataFrame[T], keys: Seq[String])(processor: (String, Iterable[T]) => U): Array[U] = {
    if (keys.isEmpty) {
      Array.empty
    } else {
      val result = Array.ofDim[U](keys.size)
      val fs = keys.zipWithIndex.map { case (key, idx) =>
        pool(result(idx) = processor(key, df.load(key)))
      }
      Await.result(Future.join(fs))
      result
    }
  }
}