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

import fr.cnrs.liris.testing.UnitSpec
import org.apache.commons.math3.distribution.{BinomialDistribution, PoissonDistribution}

import scala.util.Random

/**
 * Unit tests for [[RandomUtils]].
 */
class RandomUtilsSpec extends UnitSpec {
  behavior of "SamplingUtils"

  it should "reservoirSampleAndCount" in {
    val input = Seq.fill(100)(Random.nextInt())

    // input size < k
    val SampleAndCount(sample1, count1) = RandomUtils.reservoirSampleAndCount(input.iterator, 150)
    count1 shouldBe 100
    input shouldBe sample1.toSeq

    // input size == k
    val SampleAndCount(sample2, count2) = RandomUtils.reservoirSampleAndCount(input.iterator, 100)
    count2 shouldBe 100
    input shouldBe sample2.toSeq

    // input size > k
    val SampleAndCount(sample3, count3) = RandomUtils.reservoirSampleAndCount(input.iterator, 10)
    count3 shouldBe 100
    sample3.length shouldBe 10
  }

  it should "computeFraction" in {
    // test that the computed fraction guarantees enough data points
    // in the sample with a failure rate <= 0.0001
    val n = 100000

    for (s <- 1 to 15) {
      val frac = RandomUtils.computeFractionForSampleSize(s, n, withReplacement = true)
      val poisson = new PoissonDistribution(frac * n)
      poisson.inverseCumulativeProbability(0.0001) shouldBe >=(s) // Computed fraction is too low
    }
    for (s <- List(20, 100, 1000)) {
      val frac = RandomUtils.computeFractionForSampleSize(s, n, withReplacement = true)
      val poisson = new PoissonDistribution(frac * n)
      poisson.inverseCumulativeProbability(0.0001) shouldBe >=(s) // Computed fraction is too low
    }
    for (s <- List(1, 10, 100, 1000)) {
      val frac = RandomUtils.computeFractionForSampleSize(s, n, withReplacement = false)
      val binomial = new BinomialDistribution(n, frac)
      binomial.inverseCumulativeProbability(0.0001) * n shouldBe >=(s) // Computed fraction is too low
    }
  }
}