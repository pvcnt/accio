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

import com.twitter.util.logging.Logging
import fr.cnrs.liris.util.random._

import scala.reflect._
import scala.util.Random

abstract class DataFrame[T: ClassTag] extends Logging {
  def keys: Seq[String]

  private[sparkle] def load(key: String): Iterator[T]

  private[sparkle] def env: SparkleEnv

  /**
   * Return a sampled subset of this dataframe.
   *
   * @param withReplacement Whether elements can be sampled multiple times (replaced when sampled out).
   * @param fraction        Expected size of the sample as a fraction of this collection's size.
   *                        Without replacement: probability that each element is chosen; fraction must be [0, 1].
   *                        With replacement: expected number of times each element is chosen; fraction must be >= 0.
   * @param seed            Seed for the random number generator.
   */
  def sample(withReplacement: Boolean, fraction: Double, seed: Long = Random.nextLong): DataFrame[T] = {
    require(fraction >= 0.0, s"Negative fraction value: $fraction")
    if (withReplacement) {
      new SampleDataFrame[T](this, new PoissonSampler[T](fraction), seed)
    } else {
      new SampleDataFrame[T](this, new BernoulliSampler[T](fraction), seed)
    }
  }

  def filter(fn: T => Boolean): DataFrame[T] = {
    new MapPartitionsDataFrame[T, T](this, (_, it) => it.filter(fn))
  }

  def mapPartitions[U: ClassTag](fn: Iterator[T] => Iterator[U]): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, it) => fn(it))
  }

  def mapPartitionsWithKey[U: ClassTag](fn: (String, Iterator[T]) => Iterator[U]): DataFrame[U] = {
    new MapPartitionsDataFrame(this, fn)
  }

  def map[U: ClassTag](fn: T => U): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, it) => it.map(fn))
  }

  def flatMap[U: ClassTag](fn: T => Iterable[U]): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, it) => it.flatMap(fn))
  }

  /**
   * Zips this RDD with another one, returning key-value pairs with the first element in each RDD,
   * second element in each RDD, etc. Assumes that the two RDDs have the *same number of
   * partitions* and the *same number of elements in each partition* (e.g. one was made through
   * a map on the other).
   */
  def zip[U: ClassTag](other: DataFrame[U]): DataFrame[(T, U)] = {
    zipPartitions[U, (T, U)](other) { (thisIter: Iterator[T], otherIter: Iterator[U]) =>
      new Iterator[(T, U)] {
        override def hasNext: Boolean = (thisIter.hasNext, otherIter.hasNext) match {
          case (true, true) => true
          case (false, false) => false
          case _ => throw new RuntimeException("Can only zip DataFrames with the same number of elements in each partition")
        }

        override def next(): (T, U) = (thisIter.next(), otherIter.next())
      }
    }
  }

  /**
   * Zip this RDD's partitions with one (or more) RDD(s) and return a new RDD by
   * applying a function to the zipped partitions. Assumes that all the RDDs have the
   * *same number of partitions*, but does *not* require them to have the same number
   * of elements in each partition.
   */
  def zipPartitions[U: ClassTag, V: ClassTag](other: DataFrame[U])(fn: (Iterator[T], Iterator[U]) => Iterator[V]): DataFrame[V] = {
    new ZipPartitionsDataFrame(this, other, fn)
  }

  def count(): Long = {
    env.submit[T, Long](this, keys, (_, it) => it.size).sum
  }

  def count(fn: T => Boolean): Long = {
    env.submit[T, Long](this, keys, (_, it) => it.count(fn)).sum
  }

  def reduce(fn: (T, T) => T): T = {
    env.submit[T, T](this, keys, (_, it) => it.reduce(fn)).reduce(fn)
  }

  def collect(): Array[T] = {
    val results = env.submit[T, Array[T]](this, keys, (_, it) => it.toArray)
    Array.concat(results: _*)
  }

  /**
   * Return a fixed-size sampled subset of this data frame in an array
   *
   * @param withReplacement Whether sampling is done with replacement.
   * @param num             Size of the returned sample.
   * @param seed            Seed for the random number generator.
   */
  def takeSample(withReplacement: Boolean, num: Int, seed: Long = Random.nextLong): Array[T] = {
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
          RandomUtils.randomizeInPlace(collect(), rand)
        } else {
          val fraction = RandomUtils.computeFractionForSampleSize(num, initialCount, withReplacement)
          var samples = sample(withReplacement, fraction, rand.nextInt()).collect()

          // If the first sample didn't turn out large enough, keep trying to take samples;
          // this shouldn't happen often because we use a big multiplier for the initial size
          var numIters = 0
          while (samples.length < num) {
            logger.warn(s"Needed to re-sample due to insufficient sample size. Repeat #$numIters")
            samples = sample(withReplacement, fraction, rand.nextInt()).collect()
            numIters += 1
          }
          RandomUtils.randomizeInPlace(samples, rand).take(num)
        }
      }
    }
  }

  def first(): T = {
    val keysIt = keys.iterator
    var res: Option[T] = None
    while (res.isEmpty && keysIt.hasNext) {
      res = env
        .submit[T, Option[T]](this, Seq(keysIt.next()), (_, it) => if (it.hasNext) Some(it.next()) else None)
        .headOption
        .flatten
    }
    res.get
  }

  def foreach(fn: T => Unit): Unit = env.submit[T, Unit](this, keys, (_, it) => it.foreach(fn))

  def write(sink: DataSink[T]): Unit = {
    env.submit[T, Unit](this, keys, (key, it) => sink.write(key, it.toSeq))
  }
}