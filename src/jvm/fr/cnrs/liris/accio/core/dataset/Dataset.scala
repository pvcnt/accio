/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.accio.core.dataset

import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.random.SamplingUtils

import scala.reflect._

abstract class Dataset[T: ClassTag](val env: DatasetEnv) {
  val elementClassTag = implicitly[ClassTag[T]]

  def keys: Seq[String]

  def load(label: Option[String]): Iterator[T]

  def load(): Iterator[T] = load(None)

  def restrict(keys: Iterable[String]): Dataset[T] =
    new RestrictDataset(this, this.keys.intersect(keys.toSeq))

  def sample(proba: Double): Dataset[T] = new SampleDataset(this, proba)

  def filter(fn: T => Boolean): Dataset[T] = new FilterDataset(this, fn)

  def map[U: ClassTag](fn: T => U): Dataset[U] = new MapDataset(this, fn)

  def flatMap[U: ClassTag](fn: T => Iterable[U]): Dataset[U] = new FlatMapDataset(this, fn)

  def zip[U: ClassTag](other: Dataset[U]): Dataset[(T, U)] = new ZipDataset(this, other)

  def union(other: Dataset[T]): Dataset[T] = new UnionDataset(Seq(this, other), env)

  def count(): Long = env.submit[T, Long](this, keys, (_, it) => it.size).sum

  def min(implicit cmp: Ordering[T]): T = env.submit[T, T](this, keys, (_, it) => it.min(cmp)).min(cmp)

  def max(implicit cmp: Ordering[T]): T = env.submit[T, T](this, keys, (_, it) => it.max(cmp)).max(cmp)

  def reduce(fn: (T, T) => T): T = env.submit[T, T](this, keys, (_, it) => it.reduce(fn)).reduce(fn)

  def toArray: Array[T] = env.submit[T, Array[T]](this, keys, (_, it) => it.toArray).flatten

  def first(): T = {
    val keysIt = keys.iterator
    var res: Option[T] = None
    while (res.isEmpty && keysIt.hasNext) {
      res = env.submit[T, Option[T]](this, Seq(keysIt.next()), (_, it) => if (it.hasNext) Some(it.next()) else None)
        .headOption
        .flatten
    }
    res.get
  }

  def foreach(fn: T => Unit): Unit = env.submit[T, Unit](this, keys, (_, it) => it.foreach(fn))

  def write(sink: DataSink[T]): Unit = {
    env.submit[T, Unit](this, keys, (key, it) => sink.write(key, it))
  }

  override def toString: String =
    MoreObjects.toStringHelper(this)
      .addValue(elementClassTag)
      .toString
}

private[dataset] class RestrictDataset[T: ClassTag](inner: Dataset[T], override val keys: Seq[String])
  extends Dataset[T](inner.env) {
  override def load(label: Option[String]): Iterator[T] = {
    require(label.isEmpty || keys.contains(label.get))
    inner.load(label)
  }
}

private[dataset] class FilterDataset[T: ClassTag](inner: Dataset[T], fn: T => Boolean)
  extends Dataset[T](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[T] = inner.load(label).filter(fn)
}

private[dataset] class SampleDataset[T: ClassTag](inner: Dataset[T], proba: Double)
  extends Dataset[T](inner.env) {
  require(proba >= 0 && proba <= 1, s"Sampling probability must be in [0,1] (got $proba)")

  override val keys = SamplingUtils.sampleUniform(inner.keys, proba)

  override def load(label: Option[String]): Iterator[T] = {
    require(label.isEmpty || keys.contains(label.get))
    inner.load(label)
  }
}

private[dataset] class MapDataset[T, U: ClassTag](inner: Dataset[T], fn: T => U)
  extends Dataset[U](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[U] = inner.load(label).map(fn)
}

private[dataset] class FlatMapDataset[T, U: ClassTag](inner: Dataset[T], fn: T => Iterable[U])
  extends Dataset[U](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[U] = inner.load(label).flatMap(fn)
}

private[dataset] class UnionDataset[T: ClassTag](datasets: Iterable[Dataset[T]], env: DatasetEnv)
  extends Dataset[T](env) {
  override def keys: Seq[String] = datasets.flatMap(_.keys).toSeq.distinct.sorted

  override def load(label: Option[String]): Iterator[T] =
    datasets.map(_.load(label)).foldLeft(Iterator.empty: Iterator[T])(_ ++ _)
}

private[dataset] class ZipDataset[T: ClassTag, U: ClassTag](first: Dataset[T], other: Dataset[U])
  extends Dataset[(T, U)](first.env) {
  override def keys: Seq[String] = first.keys

  override def load(label: Option[String]): Iterator[(T, U)] =
    first.load(label).zip(other.load(label))
}