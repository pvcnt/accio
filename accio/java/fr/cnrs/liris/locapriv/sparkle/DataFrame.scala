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

package fr.cnrs.liris.locapriv.sparkle

import com.twitter.util.logging.Logging
import fr.cnrs.liris.util.Identified
import fr.cnrs.liris.util.random._

import scala.collection.mutable
import scala.reflect._
import scala.util.Random

abstract class DataFrame[T: ClassTag](val env: SparkleEnv) extends Logging {
  val elementClassTag: ClassTag[T] = implicitly[ClassTag[T]]

  def keys: Seq[String]

  def load(key: String): Iterator[T]

  /**
   * Return a sampled subset of this RDD.
   *
   * @param withReplacement Whether elements can be sampled multiple times (replaced when sampled out).
   * @param fraction        Expected size of the sample as a fraction of this collection's size.
   *                        Without replacement: probability that each element is chosen; fraction must be [0, 1].
   *                        With replacement: expected number of times each element is chosen; fraction must be >= 0.
   * @param seed            Seed for the random number generator.
   */
  def sample(withReplacement: Boolean, fraction: Double, seed: Long = RandomUtils.random.nextLong): DataFrame[T] = {
    require(fraction >= 0.0, s"Negative fraction value: $fraction")
    if (withReplacement) {
      new SampleDataFrame[T](this, new PoissonSampler[T](fraction), seed)
    } else {
      new SampleDataFrame[T](this, new BernoulliSampler[T](fraction), seed)
    }
  }

  def filter(fn: T => Boolean): DataFrame[T] = new FilterDataFrame(this, fn)

  def map[U: ClassTag](fn: T => U): DataFrame[U] = new FlatMapDataFrame(this, (el: T) => Seq(fn(el)))

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
  def takeSample(
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

  def take(n: Int): Array[T] = {
    if (keys.isEmpty) {
      Array.empty
    } else {
      var results = mutable.ArrayBuffer.empty[T] ++ load(keys.head).toSeq
      var splitsUsed = 1
      var splitsEstimate = Math.ceil(n.toDouble / results.size).toInt
      while (results.size < n && splitsUsed < keys.size) {
        val coll = mutable.Map[String, Seq[T]]()
        env.submit(this, keys.slice(splitsUsed, splitsEstimate), (key: String, it: Iterator[T]) => {
          val result = load(key).toSeq
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

  def write(sink: DataSink[T]): Unit = {
    val identifiable = classOf[Identified].isAssignableFrom(elementClassTag.runtimeClass)
    env.submit[T, Unit](this, keys, (key, it) => {
      if (identifiable) {
        //TODO: Get a rid of that once for all!
        it.toSeq
          .groupBy(_.asInstanceOf[Identified].id)
          .foreach { case (actualKey, elements) => sink.write(actualKey, elements) }
      } else {
        sink.write(key, it.toSeq)
      }
    })
  }
}