/*
 * This code comes from the Apache Spark project and is subject to the following license.
 *
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

object SamplingUtils {
  /**
   * Sample uniformly a sequence by keeping each element with the same probability.
   *
   * @param input Input sequence
   * @param proba Probability to keep each element
   * @param seed  Initialization seed
   * @tparam T Type of elements
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
   * Sample a given number of elements of a sequence.
   *
   * @param input Input sequence
   * @param n     Maximum number of elements to keep
   * @tparam T Type of elements
   */
  def sampleN[T](input: Seq[T], n: Int): Seq[T] = {
    require(n >= 0, s"n must be >= 0 (got $n)")
    val count = input.size
    if (count <= n) {
      input
    } else {
      val outOf = count.toDouble / n
      input.zipWithIndex.filter { case (el, idx) => (idx % outOf) < 1 }.map(_._1)
    }
  }

  /**
   * Reservoir sampling implementation that also returns the input size.
   *
   * @param input input size
   * @param k     reservoir size
   * @param seed  random seed
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
}

case class SampleAndCount[T](sample: Array[T], count: Int)