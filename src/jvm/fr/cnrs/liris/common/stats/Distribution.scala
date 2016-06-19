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

package fr.cnrs.liris.common.stats

/**
 * Compact representation of a distribution of values. Values must have an ordering defined.
 *
 * @param dist A mapping between possible values and their number of occurrences
 * @tparam T Value type
 */
class Distribution[T: Ordering] private(dist: Map[T, Int]) {
  def min: T = dist.keys.min

  def max: T = dist.keys.max

  /**
   * Return unique and sorted values.
   */
  def distinctValues: Seq[T] = dist.keys.toSeq.sorted

  /**
   * Expand this distribution into a list of all values.
   */
  def values: Seq[T] = distinctValues.flatMap(key => Seq.fill(count(key))(key))

  /**
   * Return the number of occurrences of a specific value.
   *
   * @param value A value
   */
  def count(value: T): Int = dist.getOrElse(value, 0)

  /**
   * Return the number of occurrences for a range of values.
   *
   * @param from  Range start (inclusive)
   * @param to Range end (inclusive)
   */
  def count(from: T, to: T): Int = {
    val ordering = implicitly[Ordering[T]]
    dist.filterKeys(key => ordering.gteq(key, from) && ordering.lteq(key, to)).values.sum
  }

  /**
   * Return the total number of values.
   */
  def size: Int = dist.values.sum

  def nonEmpty: Boolean = dist.values.exists(_ > 0)

  def isEmpty: Boolean = dist.values.forall(_ == 0)

  def isSingleValue: Boolean = dist.keys.size == 1

  def *(factor: Int): Distribution[T] =
    new Distribution(dist.map { case (k, v) => k -> v * factor })

  def ++(other: Distribution[T]): Distribution[T] =
    new Distribution((distinctValues ++ other.distinctValues).distinct.sorted.map(key => key -> (count(key) + other.count(key))).toMap)

  def toSeq: Seq[(T, Int)] = dist.toSeq.sortBy(_._1)
}

object Distribution {
  def empty[T: Ordering]: Distribution[T] = new Distribution(Map.empty)

  def apply[T: Ordering](dist: Map[T, Int]): Distribution[T] = {
    require(dist.values.forall(_ >= 0))
    new Distribution(dist)
  }

  def apply[T: Ordering](values: Seq[T]): Distribution[T] = {
    val dist = values.groupBy(identity).map { case (k, v) => k -> v.size }
    new Distribution(dist)
  }

  implicit def toDoubleDistribution(dist: Distribution[Double]): DoubleDistribution =
    new DoubleDistribution(dist)
}

class DoubleDistribution(dist: Distribution[Double]) {
  def buckets(nbBuckets: Int): Seq[(Double, Int)] = {
    if (dist.isEmpty) {
      Seq.empty
    } else {
      buckets(dist.distinctValues.head, dist.distinctValues.last, nbBuckets)
    }
  }

  def buckets(min: Double, max: Double, nbBuckets: Int): Seq[(Double, Int)] = {
    require(min <= max)
    require(nbBuckets > 0)
    if (min == max) {
      Seq(min -> dist.size)
    } else {
      val step = (max - min) / nbBuckets
      (min to max by step).sliding(2).map { case Seq(from, until) =>
        val count = dist.count(from, until)
        until -> count
      }.toSeq
    }
  }

  def cdf(nbSteps: Int): Seq[(Double, Double)] = {
    if (dist.isEmpty) {
      Seq.empty
    } else {
      cdf(dist.distinctValues.head, dist.distinctValues.last, nbSteps)
    }
  }

  def cdf(min: Double, max: Double, nbSteps: Int): Seq[(Double, Double)] = {
    require(min <= max)
    require(nbSteps > 0)
    if (min == max) {
      Seq(min -> 1d)
    } else {
      val by = (max - min) / nbSteps
      val n = dist.size
      if (n > 0) {
        (min until max by by).map(to => to -> (dist.count(min, to).toDouble / n)) ++
            Seq(max -> (dist.count(min, max).toDouble / n))
      } else {
        Seq.empty
      }
    }
  }

  def toStats: AggregatedStats = AggregatedStats(dist)
}