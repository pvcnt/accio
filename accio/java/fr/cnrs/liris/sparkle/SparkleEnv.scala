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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * A Sparkle environment is responsible for the execution of parallel tasks on [[DataFrame]]s.
 *
 * @param parallelism Parallelism level (i.e., number of cores to use).
 */
final class SparkleEnv(parallelism: Int) extends Logging {
  require(parallelism > 0, s"Parallelism level must be > 0 (got $parallelism)")
  private[this] val executor = {
    if (1 == parallelism) {
      Executors.newSingleThreadExecutor
    } else {
      Executors.newWorkStealingPool(parallelism)
    }
  }
  private[this] implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
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
   * @param data List of keys and items.
   * @tparam T Elements' type.
   */
  def parallelize[T: Encoder](data: Iterable[T]): DataFrame[T] = {
    if (data.isEmpty) {
      emptyDataFrame
    } else {
      val numPartitions = math.ceil(data.size.toDouble / parallelism).toInt
      new MemoryDataFrame(data.toSeq, numPartitions, this, implicitly[Encoder[T]])
    }
  }

  def emptyDataFrame[T: Encoder]: DataFrame[T] = new EmptyDataFrame(this, implicitly[Encoder[T]])

  def read[T: Encoder]: DataFrameReader[T] = new DataFrameReader(this, implicitly[Encoder[T]])

  /**
   * Clean and stop this environment. It will not be usable after. This method is blocking.
   */
  def stop(): Unit = synchronized {
    if (!executor.isTerminated) {
      executor.shutdown()
      while (!executor.isTerminated) {
        executor.awaitTermination(100, TimeUnit.MILLISECONDS)
      }
    }
  }

  /**
   * Submit a job to this environment.
   *
   * @param frame
   * @param keys
   * @param processor
   * @tparam T
   * @tparam U
   */
  private[sparkle] def submit[T, U: ClassTag](frame: DataFrame[T], keys: Seq[String])(processor: (String, Seq[T]) => U): Array[U] = {
    if (keys.isEmpty) {
      Array.empty
    } else if (keys.size == 1) {
      Array(processor(keys.head, frame.load(keys.head)))
    } else {
      val futures = keys.map(key => Future(processor(key, frame.load(key))))
      val future = Future.sequence(futures).map(_.toArray)
      Await.ready[Array[U]](future, Duration.Inf)
      future.value.get match {
        case Success(res) => res
        case Failure(e) => throw e
      }
    }
  }
}