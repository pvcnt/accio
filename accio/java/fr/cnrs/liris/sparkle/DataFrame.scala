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
import fr.cnrs.liris.sparkle.format.Encoder
import fr.cnrs.liris.util.random._

import scala.reflect.ClassTag
import scala.util.Random

trait DataFrame[T] extends Logging {
  def keys: Seq[String]

  private[sparkle] def load(key: String): Seq[T]

  private[sparkle] def env: SparkleEnv

  private[sparkle] def encoder: Encoder[T]

  private[this] implicit def classTag: ClassTag[T] = encoder.classTag

  def repartition(numPartitions: Int): DataFrame[T] = {
    new PartitionedDataFrame(this, numPartitions)
  }

  /**
   * Return a sampled subset of this dataframe.
   *
   * @param fraction Probability that each element is chosen; fraction must be [0, 1].
   * @param seed     Seed for the random number generator.
   */
  def sample(fraction: Double, seed: Long = Random.nextLong): DataFrame[T] = {
    require(fraction >= 0.0, s"Negative fraction value: $fraction")
    new SampleDataFrame[T](this, new BernoulliSampler[T](fraction), seed)
  }

  def filter(fn: T => Boolean): DataFrame[T] = {
    new MapPartitionsDataFrame[T, T](this, (_, seq) => seq.filter(fn), encoder)
  }

  def mapPartitions[U: Encoder](fn: Seq[T] => Seq[U]): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, seq) => fn(seq), implicitly[Encoder[U]])
  }

  def mapPartitionsWithKey[U: Encoder](fn: (String, Seq[T]) => Seq[U]): DataFrame[U] = {
    new MapPartitionsDataFrame(this, fn, implicitly[Encoder[U]])
  }

  def map[U: Encoder](fn: T => U): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, seq) => seq.map(fn), implicitly[Encoder[U]])
  }

  def flatMap[U: Encoder](fn: T => Iterable[U]): DataFrame[U] = {
    new MapPartitionsDataFrame[T, U](this, (_, seq) => seq.flatMap(fn), implicitly[Encoder[U]])
  }

  /**
   * Zips this RDD with another one, returning key-value pairs with the first element in each RDD,
   * second element in each RDD, etc. Assumes that the two RDDs have the *same number of
   * partitions* and the *same number of elements in each partition* (e.g. one was made through
   * a map on the other).
   */
  // We do not define encoders for tuples, so not possible for now (but easily doable)
  /*def zip[U: Encoder](other: DataFrame[U]): DataFrame[(T, U)] = {
    zipPartitions[U, (T, U)](other) { (thisSeq: Seq[T], otherSeq: Seq[U]) =>
      thisSeq.zip(otherSeq)
    }
  }*/

  /**
   * Zip this RDD's partitions with one (or more) RDD(s) and return a new RDD by
   * applying a function to the zipped partitions. Assumes that all the RDDs have the
   * *same number of partitions*, but does *not* require them to have the same number
   * of elements in each partition.
   */
  def zipPartitions[U, V: Encoder](other: DataFrame[U])(fn: (Seq[T], Seq[U]) => Seq[V]): DataFrame[V] = {
    new ZipPartitionsDataFrame(this, other, fn, implicitly[Encoder[V]])
  }

  def count(): Long = {
    env.submit[T, Long](this, keys, (_, seq) => seq.size).sum
  }

  def count(fn: T => Boolean): Long = {
    env.submit[T, Long](this, keys, (_, seq) => seq.count(fn)).sum
  }

  def reduce(fn: (T, T) => T): T = {
    env.submit[T, T](this, keys, (_, seq) => seq.reduce(fn)).reduce(fn)
  }

  def collect(): Array[T] = {
    val results = env.submit[T, Array[T]](this, keys, (_, seq) => seq.toArray)
    Array.concat(results: _*)
  }

  /**
   * Return a fixed-size sampled subset of this data frame in an array
   *
   * @param num  Size of the returned sample.
   * @param seed Seed for the random number generator.
   */
  def takeSample(num: Int, seed: Long = Random.nextLong): Array[T] = {
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
        if (num >= initialCount) {
          RandomUtils.randomizeInPlace(collect(), rand)
        } else {
          val fraction = RandomUtils.computeFractionForSampleSize(num, initialCount)
          var samples = sample(fraction, rand.nextInt()).collect()

          // If the first sample didn't turn out large enough, keep trying to take samples;
          // this shouldn't happen often because we use a big multiplier for the initial size
          var numIters = 0
          while (samples.length < num) {
            logger.warn(s"Needed to re-sample due to insufficient sample size. Repeat #$numIters")
            samples = sample(fraction, rand.nextInt()).collect()
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
        .submit[T, Option[T]](this, Seq(keysIt.next()), (_, seq) => seq.headOption)
        .headOption
        .flatten
    }
    res.get
  }

  def foreach(fn: T => Unit): Unit = {
    env.submit[T, Unit](this, keys, (_, seq) => seq.foreach(fn))
  }

  def foreachPartition(fn: Seq[T] => Unit): Unit = {
    env.submit[T, Unit](this, keys, (_, seq) => fn(seq))
  }

  def foreachPartitionWithKey(fn: (String, Seq[T]) => Unit): Unit = {
    env.submit[T, Unit](this, keys, fn)
  }

  def write: DataFrameWriter[T] = new DataFrameWriter(this)
}

object DataFrame {
  implicit def toNumericOps[T: Numeric](df: DataFrame[T]): NumericDataFrameOps[T] = {
    new NumericDataFrameOps(df)
  }
}