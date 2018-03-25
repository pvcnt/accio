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

package fr.cnrs.liris.locapriv.ops

import breeze.stats.DescriptiveStats._
import breeze.stats._

case class AggregatedStats(
  n: Long,
  min: Double,
  max: Double,
  avg: Double,
  stddev: Double,
  p25: Double,
  p50: Double,
  p75: Double,
  p90: Double,
  p95: Double,
  p99: Double) {

  def median: Double = p50

  def range: Double = max - min
}

object AggregatedStats {
  def empty: AggregatedStats = AggregatedStats(0, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d)

  def apply(values: Seq[Double]): AggregatedStats = apply(values.toArray)

  def apply(values: Array[Double]): AggregatedStats = {
    if (values.isEmpty) {
      empty
    } else {
      AggregatedStats(
        n = values.length,
        min = values.min,
        max = values.max,
        avg = mean(values),
        stddev = stddev(values),
        p25 = percentile(values, .25),
        p50 = percentile(values, .50),
        p75 = percentile(values, .75),
        p90 = percentile(values, .90),
        p95 = percentile(values, .95),
        p99 = percentile(values, .99))
    }
  }
}