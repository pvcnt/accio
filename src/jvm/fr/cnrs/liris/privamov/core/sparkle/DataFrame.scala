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
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.random._

import scala.collection.mutable
import scala.reflect._
import scala.util.Random

abstract class DataFrame[T: ClassTag](val env: SparkleEnv) extends LazyLogging {
  val elementClassTag = implicitly[ClassTag[T]]

  def keys: Seq[String]

  def load(label: Option[String]): Iterator[T]

  def load(): Iterator[T] = load(None)

  def restrict(keys: Iterable[String]): DataFrame[T] =
    new RestrictDataFrame(this, this.keys.intersect(keys.toSeq))

  /**
   * Return a sampled subset of this RDD.
   *
   * @param withReplacement Whether elements can be sampled multiple times (replaced when sampled out).
   * @param fraction        Expected size of the sample as a fraction of this collection's size.
   *                        Without replacement: probability that each element is chosen; fraction must be [0, 1].
   *                        With replacement: expected number of times each element is chosen; fraction must be >= 0.
   * @param seed            Seed for the random number generator.
   */
  final def sample(
    withReplacement: Boolean,
    fraction: Double,
    seed: Long = RandomUtils.random.nextLong): DataFrame[T] = {
    require(fraction >= 0.0, s"Negative fraction value: $fraction")
    if (withReplacement) {
      new SampleDataFrame[T](this, new PoissonSampler[T](fraction), seed)
    } else {
      new SampleDataFrame[T](this, new BernoulliSampler[T](fraction), seed)
    }
  }

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

  /**
   * Return a fixed-size sampled subset of this data frame in an array
   *
   * @param withReplacement Whether sampling is done with replacement.
   * @param num             Size of the returned sample.
   * @param seed            Seed for the random number generator.
   */
  final def takeSample(
    withReplacement: Boolean,
    num: Int,
    seed: Long = RandomUtils.random.nextLong): Array[T] = {
    val numStDev = 10.0
    require(num >= 0, s"Negative number of elements requested: $num")
    require(num <= (Int.MaxValue - (numStDev * math.sqrt(Int.MaxValue)).toInt),
      s"Cannot support a sample size > Int.MaxValue - $numStDev * math.sqrt(Int.MaxValue)")

    if (num == 0) {
      Array.empty
    } else {
      val initialCount = count()
      if (initialCount == 0) {
        Array.empty
      } else {
        val rand = new Random(seed)
        if (!withReplacement && num >= initialCount) {
          RandomUtils.randomizeInPlace(toArray, rand)
        } else {
          val fraction = RandomUtils.computeFractionForSampleSize(num, initialCount, withReplacement)
          var samples = sample(withReplacement, fraction, rand.nextInt()).toArray

          // If the first sample didn't turn out large enough, keep trying to take samples;
          // this shouldn't happen often because we use a big multiplier for the initial size
          var numIters = 0
          while (samples.length < num) {
            logger.warn(s"Needed to re-sample due to insufficient sample size. Repeat #$numIters")
            samples = sample(withReplacement, fraction, rand.nextInt()).toArray
            numIters += 1
          }
          RandomUtils.randomizeInPlace(samples, rand).take(num)
        }
      }
    }
  }

  final def take(n: Int): Array[T] = {
    if (keys.isEmpty) {
      Array.empty
    } else {
      var results = mutable.ArrayBuffer.empty[T] ++ load(Some(keys.head)).toSeq
      var splitsUsed = 1
      var splitsEstimate = Math.ceil(n.toDouble / results.size).toInt
      while (results.size < n && splitsUsed < keys.size) {
        val coll = mutable.Map[String, Seq[T]]()
        env.submit(this, keys.slice(splitsUsed, splitsEstimate), (key: String, it: Iterator[T]) => {
          val result = load(Some(key)).toSeq
          coll synchronized {
            coll(key) = result
          }
        })
        results ++= coll.toSeq.sortBy(_._1).flatMap(_._2)
        splitsUsed = splitsEstimate
        splitsEstimate += Math.ceil(n.toDouble / results.size / splitsUsed).toInt
      }
      if (results.size <= n) {
        results.toArray
      } else {
        results.take(n).toArray
      }
    }
  }

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

private[sparkle] class SampleDataFrame[T: ClassTag](inner: DataFrame[T], sampler: RandomSampler[T, T], seed: Long)
  extends DataFrame[T](inner.env) {
  private[this] val seeds = {
    val random = new Random(seed)
    inner.keys.map(key => key -> random.nextLong).toMap
  }

  override def keys: Seq[String] = inner.keys

  override def load(label: Option[String]): Iterator[T] = {
    label match {
      case Some(key) => sample(key)
      case None => keys.iterator.flatMap(sample)
    }
  }

  private def sample(key: String) = {
    val aSampler = sampler.clone
    aSampler.setSeed(seeds(key))
    aSampler.sample(inner.load(Some(key)))
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