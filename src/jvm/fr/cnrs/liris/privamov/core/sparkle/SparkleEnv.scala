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

import java.util.concurrent.Executors

import com.google.common.base.Stopwatch
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.privamov.core.io.DataSource

import scala.collection.immutable.ListMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

class SparkleEnv(level: Int) extends StrictLogging {
  require(level > 0, s"Parallelism level must be > 0 (got $level)")
  private[this] implicit val ec = if (1 == level) {
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
  } else {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(level))
  }

  def parallelize[T: ClassTag](data: (String, Iterable[T])*): DataFrame[T] =
    new ParallelCollectionDataFrame(ListMap(data.map { case (key, value) => key -> value.toSeq }: _*), this)

  def parallelize[T: ClassTag](values: T*)(indexer: T => String): DataFrame[T] =
    new ParallelCollectionDataFrame(values.groupBy(indexer), this)

  /**
   * Create a new dataset from an index, a record reader and a decoder.
   *
   * @param source Data source
   * @tparam T Type of elements
   */
  def read[T: ClassTag](source: DataSource[T]): DataFrame[T] = new SourceDataFrame(source, this)

  def union[T: ClassTag](datasets: DataFrame[T]*): DataFrame[T] = new UnionDataFrame(datasets, this)

  def stop(): Unit = {}

  private[sparkle] def submit[T, U: ClassTag](dataset: DataFrame[T], keys: Seq[String], processor: (String, Iterator[T]) => U): Array[U] = {
    if (keys.isEmpty) {
      Array.empty
    } else if (keys.size == 1) {
      Array(processor(keys.head, dataset.load(keys.head)))
    } else {
      val futures = keys.map(key => Future(processor(key, dataset.load(key))))
      Await.result[Array[U]](Future.sequence(futures).map(_.toArray), Duration.Inf)
    }
  }
}

private class ParallelCollectionDataFrame[T: ClassTag](data: Map[String, Seq[T]], env: SparkleEnv) extends DataFrame[T](env) {
  override def keys: Seq[String] = data.keySet.toSeq

  override def load(key: String): Iterator[T] = if (data.contains(key)) data(key).iterator else Iterator.empty
}

/**
 * A dataframe loading its data on the fly using a data source.
 *
 * @param source Data source
 * @tparam T Type of elements being read
 */
private class SourceDataFrame[T: ClassTag](source: DataSource[T], env: SparkleEnv) extends DataFrame[T](env) {
  override lazy val keys: Seq[String] = {
    val w = Stopwatch.createStarted()
    val k = source.keys
    println(s"read keys in $w")
    k
  }

  override def load(key: String): Iterator[T] = {
    val w = Stopwatch.createStarted()
    val it = source.read(key).iterator
    println(s"read key '$key' in $w")
    it
  }
}
