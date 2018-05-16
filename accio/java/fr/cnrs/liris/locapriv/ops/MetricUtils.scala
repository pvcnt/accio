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

object MetricUtils {
  case class FscoreValue(id: String, precision: Double, recall: Double, fscore: Double)

  case class StatsValue(
    id: String,
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
    p99: Double)

  private[ops] def fscore(id: String, reference: Int, result: Int, matched: Int): FscoreValue = {
    require(matched <= reference, s"Matched points must be less than reference points (got $matched and $reference)")
    require(matched <= result, s"Matched points must be less than result points (got $matched and $result)")
    val precision = if (result != 0) matched.toDouble / result else 1
    val recall = if (reference != 0) matched.toDouble / reference else 0
    val fscore = if (precision > 0 && recall > 0) {
      2 * precision * recall / (precision + recall)
    } else {
      0d
    }
    FscoreValue(id, precision, recall, fscore)
  }

  private[ops] def stats(id: String, values: Iterable[Double]): StatsValue = stats(id, values.toArray)

  private[ops] def stats(id: String, values: Array[Double]): StatsValue = {
    if (values.isEmpty) {
      StatsValue(id, 0, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d)
    } else {
      StatsValue(
        id = id,
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