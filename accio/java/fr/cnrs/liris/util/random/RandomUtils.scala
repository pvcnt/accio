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

package fr.cnrs.liris.util.random

import scala.util.Random

/**
 * Utils related to randomness.
 */
object RandomUtils {
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
   * Returns a sampling rate that guarantees a sample of size >= sampleSizeLowerBound 99.99% of the time.
   *
   * How the sampling rate is determined:
   * Let p = num / total, where num is the sample size and total is the total number of
   * datapoints in the RDD. We're trying to compute q > p such that
   * - when sampling with replacement, we're drawing each datapoint with prob_i ~ Pois(q), we're
   * drawing each datapoint with prob_i ~ Binomial(total, fraction) and our choice of q guarantees
   * 1-delta, or 0.9999 success rate, where success rate is defined the same as in sampling with replacement.
   *
   * The smallest sampling rate supported is 1e-10 (in order to avoid running into the limit of the
   * RNG's resolution).
   *
   * @param sampleSizeLowerBound Sample size.
   * @param total                Size of the collection.
   * @return A sampling rate that guarantees sufficient sample size with 99.99% success rate.
   */
  def computeFractionForSampleSize(sampleSizeLowerBound: Int, total: Long): Double = {
    val fraction = sampleSizeLowerBound.toDouble / total
    BinomialBounds.getUpperBound(1e-4, total, fraction)
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