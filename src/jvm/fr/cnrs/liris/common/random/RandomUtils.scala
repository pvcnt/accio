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

// Parts of this file come from the Apache Spark project, subject to the following license:
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cnrs.liris.common.random

import scala.reflect.ClassTag
import scala.util.Random

/**
 * Utils related to randomness.
 */
object RandomUtils {
  /**
   * A shared [[scala.util.Random]] instance.
   */
  val random = new Random

  /**
   * Sample uniformly a sequence by keeping each element with the same probability.
   *
   * @param input Input sequence.
   * @param proba Probability to keep each element.
   * @param seed  Seed.
   * @tparam T Elements' type.
   */
  def sampleUniform[T](input: Seq[T], proba: Double, seed: Long = Random.nextLong()): Seq[T] = {
    require(proba >= 0 && proba <= 1, s"proba must be in [0,1] (got $proba )")
    if (proba == 1) {
      input
    } else if (proba == 0) {
      Seq.empty
    } else {
      val rnd = new Random(seed)
      input.filter(_ => rnd.nextDouble() <= proba)
    }
  }

  /**
   * Draw a random element from a sequence.
   *
   * @param xs   Non-empty sequence.
   * @param seed Seed.
   * @tparam T Elements' type.
   */
  def randomElement[T](xs: Seq[T], seed: Long = random.nextLong): T = {
    require(xs.nonEmpty, "Cannot draw a random element from an empty collection")
    if (xs.size == 1) {
      xs.head
    } else {
      val rnd = new Random(seed)
      xs(rnd.nextInt(xs.size))
    }
  }

  /**
   * Draw a random element from an array.
   *
   * @param xs Non-empty array
   * @tparam T Elements' type.
   */
  def randomElement[T](xs: Array[T]): T = randomElement(xs.toSeq)

  /**
   * Draw a random element from an array.
   *
   * @param xs   Non-empty array.
   * @param seed Seed.
   * @tparam T Elements' type.
   */
  def randomElement[T](xs: Array[T], seed: Long): T = randomElement(xs.toSeq, seed)

  /**
   * Shuffle the elements of a collection into a random order, returning the
   * result in a new collection. Unlike [[scala.util.Random.shuffle]], this method
   * uses a local random number generator, avoiding inter-thread contention.
   *
   * @param input Traversable to shuffle.
   * @param rand  Random number generator.
   * @tparam T Elements' type.
   */
  def randomize[T: ClassTag](input: TraversableOnce[T], rand: Random = new Random): Seq[T] =
  randomizeInPlace(input.toArray, rand).toSeq

  /**
   * Shuffle the elements of an array into a random order, modifying the original array.
   *
   * @param input Array to shuffle.
   * @param rand  Random number generator.
   * @tparam T Elements' type.
   * @return The original array.
   */
  def randomizeInPlace[T](input: Array[T], rand: Random = new Random): Array[T] = {
    for (i <- (input.length - 1) to 1 by -1) {
      val j = rand.nextInt(i)
      val tmp = input(j)
      input(j) = input(i)
      input(i) = tmp
    }
    input
  }


  /**
   * Reservoir sampling implementation that also returns the input size.
   *
   * @param input input size
   * @param k     reservoir size
   * @param seed  random seed
   * @tparam T Elements' type.
   * @return (samples, input size)
   */
  def reservoirSampleAndCount[T: ClassTag](input: Iterator[T], k: Int, seed: Long = Random.nextLong()): SampleAndCount[T] = {
    val reservoir = new Array[T](k)
    // Put the first k elements in the reservoir.
    var i = 0
    while (i < k && input.hasNext) {
      val item = input.next()
      reservoir(i) = item
      i += 1
    }

    // If we have consumed all the elements, return them. Otherwise do the replacement.
    if (i < k) {
      // If input size < k, trim the array to return only an array of input size.
      val trimReservoir = new Array[T](i)
      System.arraycopy(reservoir, 0, trimReservoir, 0, i)
      SampleAndCount(trimReservoir, i)
    } else {
      // If input size > k, continue the sampling process.
      val rand = new XORShiftRandom(seed)
      while (input.hasNext) {
        val item = input.next()
        val replacementIndex = rand.nextInt(i)
        if (replacementIndex < k) {
          reservoir(replacementIndex) = item
        }
        i += 1
      }
      SampleAndCount(reservoir, i)
    }
  }

  def reservoirSample[T: ClassTag](input: Iterator[T], k: Int, seed: Long = Random.nextLong()): Array[T] =
    reservoirSampleAndCount(input, k, seed).sample

  /**
   * Returns a sampling rate that guarantees a sample of size >= sampleSizeLowerBound 99.99% of the time.
   *
   * How the sampling rate is determined:
   * Let p = num / total, where num is the sample size and total is the total number of
   * datapoints in the RDD. We're trying to compute q > p such that
   * - when sampling with replacement, we're drawing each datapoint with prob_i ~ Pois(q),
   * where we want to guarantee Pr[s < num] < 0.0001 for s = sum(prob_i for i from 0 to total),
   * i.e. the failure rate of not having a sufficiently large sample < 0.0001.
   * Setting q = p + 5 * sqrt(p/total) is sufficient to guarantee 0.9999 success rate for
   * num > 12, but we need a slightly larger q (9 empirically determined).
   * - when sampling without replacement, we're drawing each datapoint with prob_i
   * ~ Binomial(total, fraction) and our choice of q guarantees 1-delta, or 0.9999 success
   * rate, where success rate is defined the same as in sampling with replacement.
   *
   * The smallest sampling rate supported is 1e-10 (in order to avoid running into the limit of the
   * RNG's resolution).
   *
   * @param sampleSizeLowerBound Sample size.
   * @param total                Size of the collection.
   * @param withReplacement      Whether to sample with replacement.
   * @return A sampling rate that guarantees sufficient sample size with 99.99% success rate.
   */
  def computeFractionForSampleSize(sampleSizeLowerBound: Int, total: Long, withReplacement: Boolean): Double = {
    if (withReplacement) {
      PoissonBounds.getUpperBound(sampleSizeLowerBound) / total
    } else {
      val fraction = sampleSizeLowerBound.toDouble / total
      BinomialBounds.getUpperBound(1e-4, total, fraction)
    }
  }
}

/**
 * A sample and the total size of the original collection.
 *
 * @param sample A sample.
 * @param count  Total size of the original collection.
 * @tparam T Elements' type.
 */
case class SampleAndCount[T](sample: Array[T], count: Int)

/**
 * Utility functions that help us determine bounds on adjusted sampling rate to guarantee exact
 * sample sizes with high confidence when sampling with replacement.
 */
private object PoissonBounds {
  /**
   * Returns a lambda such that Pr[X > s] is very small, where X ~ Pois(lambda).
   */
  def getLowerBound(s: Double): Double = {
    math.max(s - numStd(s) * math.sqrt(s), 1e-15)
  }

  /**
   * Returns a lambda such that Pr[X < s] is very small, where X ~ Pois(lambda).
   *
   * @param s sample size
   */
  def getUpperBound(s: Double): Double = {
    math.max(s + numStd(s) * math.sqrt(s), 1e-10)
  }

  private def numStd(s: Double): Double = {
    // TODO: Make it tighter.
    if (s < 6.0) {
      12.0
    } else if (s < 16.0) {
      9.0
    } else {
      6.0
    }
  }
}

/**
 * Utility functions that help us determine bounds on adjusted sampling rate to guarantee exact
 * sample size with high confidence when sampling without replacement.
 */
private object BinomialBounds {
  val minSamplingRate = 1e-10

  /**
   * Returns a threshold `p` such that if we conduct n Bernoulli trials with success rate = `p`,
   * it is very unlikely to have more than `fraction * n` successes.
   */
  def getLowerBound(delta: Double, n: Long, fraction: Double): Double = {
    val gamma = -math.log(delta) / n * (2.0 / 3.0)
    fraction + gamma - math.sqrt(gamma * gamma + 3 * gamma * fraction)
  }

  /**
   * Returns a threshold `p` such that if we conduct n Bernoulli trials with success rate = `p`,
   * it is very unlikely to have less than `fraction * n` successes.
   */
  def getUpperBound(delta: Double, n: Long, fraction: Double): Double = {
    val gamma = -math.log(delta) / n
    math.min(1,
      math.max(minSamplingRate, fraction + gamma + math.sqrt(gamma * gamma + 2 * gamma * fraction)))
  }
}