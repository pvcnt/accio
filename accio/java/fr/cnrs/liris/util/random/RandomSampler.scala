// Comes from the Apache Spark project, subject to the following license:
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

import java.util.Random

import scala.reflect.ClassTag

/**
 * A pseudorandom sampler. It is possible to change the sampled item type. For example, we might
 * want to add weights for stratified sampling or importance sampling. Should only use
 * transformations that are tied to the sampler and cannot be applied after sampling.
 *
 * @tparam T item type
 * @tparam U sampled item type
 */
trait RandomSampler[T, U] extends Pseudorandom with Cloneable with Serializable {
  /**
   * take a random sample
   */
  def sample(items: Seq[T]): Seq[U]

  /**
   * return a copy of the RandomSampler object
   */
  override def clone: RandomSampler[T, U] = throw new NotImplementedError("clone() is not implemented.")
}

object RandomSampler {
  /** Default random number generator used by random samplers. */
  def newDefaultRNG: Random = new XORShiftRandom

  /**
   * Default epsilon for floating point numbers sampled from the RNG.
   * The gap-sampling compute logic requires taking log(x), where x is sampled from an RNG.
   * To guard against errors from taking log(0), a positive epsilon lower bound is applied.
   * A good value for this parameter is at or near the minimum positive floating
   * point value returned by "nextDouble()" (or equivalent), for the RNG being used.
   */
  val rngEpsilon = 5e-11

  /**
   * Sampling fraction arguments may be results of computation, and subject to floating
   * point jitter.  I check the arguments with this epsilon slop factor to prevent spurious
   * warnings for cases such as summing some numbers to get a sampling fraction of 1.000000001
   */
  val roundingEpsilon = 1e-6
}

/**
 * A sampler based on Bernoulli trials.
 *
 * @param fraction the sampling fraction, aka Bernoulli sampling probability
 * @tparam T item type
 */
class BernoulliSampler[T: ClassTag](fraction: Double) extends RandomSampler[T, T] {
  // Epsilon slop to avoid failure from floating point jitter.
  require(
    fraction >= (0.0 - RandomSampler.roundingEpsilon) && fraction <= (1.0 + RandomSampler.roundingEpsilon),
    s"Sampling fraction ($fraction) must be on interval [0, 1]")

  private val rng: Random = RandomSampler.newDefaultRNG

  override def setSeed(seed: Long): Unit = rng.setSeed(seed)

  override def sample(items: Seq[T]): Seq[T] = {
    if (fraction <= 0.0) {
      Seq.empty
    } else if (fraction >= 1.0) {
      items
    } else {
      items.filter(_ => rng.nextDouble() <= fraction)
    }
  }

  override def clone: BernoulliSampler[T] = new BernoulliSampler[T](fraction)
}