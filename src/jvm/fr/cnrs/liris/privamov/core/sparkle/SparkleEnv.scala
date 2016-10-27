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

package fr.cnrs.liris.privamov.core.sparkle

import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.privamov.core.io.DataSource

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * A Sparkle environment is responsible for the execution of parallel tasks on [[DataFrame]]s. It also provide
 * utility methods to create or manipulate datafframes.
 *
 * @param level Parallelism level (i.e., number of cores to use).
 */
class SparkleEnv(level: Int) extends StrictLogging {
  require(level > 0, s"Parallelism level must be > 0 (got $level)")
  logger.info(s"Initializing Sparkle to utilize $level cores")
  private[this] val executor = if (1 == level) Executors.newSingleThreadExecutor else Executors.newWorkStealingPool(level)
  private[this] implicit val ec = ExecutionContext.fromExecutor(executor)

  /**
   * Create a new dataframe from an in-memory collection.
   *
   * @param data List of keys and items.
   * @tparam T Elements' type.
   */
  def parallelize[T: ClassTag](data: (String, Iterable[T])*): DataFrame[T] =
  new ParallelCollectionDataFrame(ListMap(data.map { case (key, value) => key -> value.toSeq }: _*), this)

  /**
   * Create a new dataframe from an in-memory collection.
   *
   * @param values  List of items.
   * @param indexer Indexing function, extracting the key from an item.
   * @tparam T Elements' type.
   */
  def parallelize[T: ClassTag](values: T*)(indexer: T => String): DataFrame[T] =
  new ParallelCollectionDataFrame(values.groupBy(indexer), this)

  /**
   * Create a new dataframe from a data source.
   *
   * @param source Data source.
   * @tparam T Elements' type.
   */
  def read[T: ClassTag](source: DataSource[T]): DataFrame[T] = new SourceDataFrame(source, this)

  /**
   * Create a new dataframe which is the union of several dataframes.
   *
   * @param frames Frames to union.
   * @tparam T Elements' type.
   */
  def union[T: ClassTag](frames: DataFrame[T]*): DataFrame[T] = new UnionDataFrame(frames, this)

  /**
   * Clean and stop this environment. It will not be usable after. This method is blocking.
   */
  def stop(): Unit = synchronized {
    if (!executor.isTerminated) {
      executor.shutdown()
      while (!executor.isTerminated) {
        executor.awaitTermination(1, TimeUnit.SECONDS)
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
   * @throws SparkleJobException
   * @return
   */
  @throws[SparkleJobException]
  private[sparkle] def submit[T, U: ClassTag](frame: DataFrame[T], keys: Seq[String], processor: (String, Iterator[T]) => U): Array[U] = {
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
        case Failure(e) => throw new SparkleJobException(frame, e)
      }
    }
  }
}

class SparkleJobException(frame: DataFrame[_], cause: Throwable) extends Exception(s"Error while processing $frame", cause)