package fr.cnrs.liris.privamov.sparkle

import java.util.concurrent.Executors

import com.typesafe.scalalogging.StrictLogging

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
      Array(processor(keys.head, dataset.load(Some(keys.head))))
    } else {
      val futures = keys.map(key => Future(processor(key, dataset.load(Some(key)))))
      Await.result[Array[U]](Future.sequence(futures).map(_.toArray), Duration.Inf)
    }
  }
}

private class ParallelCollectionDataFrame[T: ClassTag](data: Map[String, Seq[T]], env: SparkleEnv) extends DataFrame[T](env) {
  override def keys: Seq[String] = data.keySet.toSeq

  override def load(label: Option[String]): Iterator[T] = label match {
    case Some(key) => if (data.contains(key)) data(key).iterator else Iterator.empty
    case None => data.values.iterator.map(_.iterator).flatten
  }
}

/**
 * A dataset loading its data on the fly using a data source.
 *
 * @param source Data source
 * @tparam T Type of elements being read
 */
private class SourceDataFrame[T: ClassTag](source: DataSource[T], env: SparkleEnv) extends DataFrame[T](env) {
  override def keys: Seq[String] = source.keys

  override def load(label: Option[String]): Iterator[T] = label match {
    case Some(key) => source.read(key).iterator
    case None => keys.flatMap(source.read).iterator
  }
}
