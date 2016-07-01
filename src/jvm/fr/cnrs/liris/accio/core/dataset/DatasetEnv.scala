package fr.cnrs.liris.accio.core.dataset

import java.util.concurrent.Executors

import com.typesafe.scalalogging.StrictLogging

import scala.collection.immutable.TreeMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

class DatasetEnv(level: Int) extends StrictLogging {
  require(level > 0, s"Parallelism level must be > 0 (got $level)")
  private[this] implicit val ec = if (1 == level) {
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
  } else {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(level))
  }

  def parallelize[T: ClassTag](data: (String, Iterable[T])*): Dataset[T] =
    new ParallelCollectionDataset(TreeMap(data.map { case (key, value) => key -> value.toSeq }: _*), this)

  def parallelize[T: ClassTag](values: T*)(indexer: T => String): Dataset[T] =
    new ParallelCollectionDataset(TreeMap(values.groupBy(indexer).toSeq: _*), this)

  /**
   * Create a new dataset from an index, a record reader and a decoder.
   *
   * @param source Data source
   * @tparam T Type of elements
   */
  def read[T: ClassTag](source: DataSource[T]): Dataset[T] = new SourceDataset(source, this)

  def union[T: ClassTag](datasets: Dataset[T]*): Dataset[T] = new UnionDataset(datasets, this)

  def stop(): Unit = {}

  private[dataset] def submit[T, U: ClassTag](dataset: Dataset[T], keys: Seq[String], processor: (String, Iterator[T]) => U): Array[U] = {
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

private class ParallelCollectionDataset[T: ClassTag](data: TreeMap[String, Seq[T]], env: DatasetEnv)
  extends Dataset[T](env) {
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
private class SourceDataset[T: ClassTag](source: DataSource[T], env: DatasetEnv) extends Dataset[T](env) {
  override def keys: Seq[String] = source.keys

  override def load(label: Option[String]): Iterator[T] = label match {
    case Some(key) => source.read(key).iterator
    case None => keys.flatMap(source.read).iterator
  }
}
