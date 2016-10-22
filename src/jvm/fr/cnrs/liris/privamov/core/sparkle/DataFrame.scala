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

package fr.cnrs.liris.privamov.core.sparkle

import com.google.common.base.MoreObjects
import fr.cnrs.liris.common.random.SamplingUtils

import scala.reflect._

abstract class DataFrame[T: ClassTag](val env: SparkleEnv) {
  val elementClassTag = implicitly[ClassTag[T]]

  def keys: Seq[String]

  def load(label: Option[String]): Iterator[T]

  def load(): Iterator[T] = load(None)

  def restrict(keys: Iterable[String]): DataFrame[T] =
    new RestrictDataFrame(this, this.keys.intersect(keys.toSeq))

  def sample(proba: Double): DataFrame[T] = new SampleDataFrame(this, proba)

  def filter(fn: T => Boolean): DataFrame[T] = new FilterDataFrame(this, fn)

  def map[U: ClassTag](fn: T => U): DataFrame[U] = new MapDataFrame(this, fn)

  def flatMap[U: ClassTag](fn: T => Iterable[U]): DataFrame[U] = new FlatMapDataFrame(this, fn)

  def zip[U: ClassTag](other: DataFrame[U]): DataFrame[(T, U)] = new ZipDataFrame(this, other)

  def union(other: DataFrame[T]): DataFrame[T] = new UnionDataFrame(Seq(this, other), env)

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

  def write(sink: DataSink[T]): Unit = env.submit[T, Unit](this, keys, (key, it) => sink.write(it))

  override def toString: String =
    MoreObjects.toStringHelper(this)
        .addValue(elementClassTag)
        .toString
}

private[sparkle] class RestrictDataFrame[T: ClassTag](inner: DataFrame[T], override val keys: Seq[String])
    extends DataFrame[T](inner.env) {
  override def load(label: Option[String]): Iterator[T] = label match {
    case Some(key) => if (keys.contains(key)) inner.load(Some(key)) else Iterator.empty
    case None => keys.map(key => load(Some(key))).fold(Iterator.empty)(_ ++ _)
  }
}

private[sparkle] class FilterDataFrame[T: ClassTag](inner: DataFrame[T], fn: T => Boolean)
    extends DataFrame[T](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[T] = inner.load(label).filter(fn)
}

private[sparkle] class SampleDataFrame[T: ClassTag](inner: DataFrame[T], proba: Double)
    extends DataFrame[T](inner.env) {
  require(proba >= 0 && proba <= 1, s"Sampling probability must be in [0,1] (got $proba)")

  override val keys = SamplingUtils.sampleUniform(inner.keys, proba)

  override def load(label: Option[String]): Iterator[T] = {
    require(label.isEmpty || keys.contains(label.get))
    inner.load(label)
  }
}

private[sparkle] class MapDataFrame[T, U: ClassTag](inner: DataFrame[T], fn: T => U)
    extends DataFrame[U](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[U] = inner.load(label).map(fn)
}

private[sparkle] class FlatMapDataFrame[T, U: ClassTag](inner: DataFrame[T], fn: T => Iterable[U])
    extends DataFrame[U](inner.env) {
  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[U] = inner.load(label).flatMap(fn)
}

private[sparkle] class UnionDataFrame[T: ClassTag](datasets: Iterable[DataFrame[T]], env: SparkleEnv)
    extends DataFrame[T](env) {
  override def keys: Seq[String] = datasets.flatMap(_.keys).toSeq.distinct.sorted

  override def load(label: Option[String]): Iterator[T] =
    datasets.map(_.load(label)).foldLeft(Iterator.empty: Iterator[T])(_ ++ _)
}

private[sparkle] class ZipDataFrame[T: ClassTag, U: ClassTag](first: DataFrame[T], other: DataFrame[U])
    extends DataFrame[(T, U)](first.env) {
  override def keys: Seq[String] = first.keys

  override def load(label: Option[String]): Iterator[(T, U)] =
    first.load(label).zip(other.load(label))
}