/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.ops

private[ops] object MetricUtils {
  def fscore(reference: Int, result: Int, matched: Int): Double = {
    val precision = this.precision(result, matched)
    val recall = this.recall(reference, matched)
    if (precision > 0 && recall > 0) {
      2 * precision * recall / (precision + recall)
    } else {
      0d
    }
  }

  def precision(result: Int, matched: Int): Double = {
    require(matched <= result, s"Matched points must be less than result points (got $matched and $result)")
    if (result != 0) matched.toDouble / result else 1
  }

  def recall(reference: Int, matched: Int): Double = {
    require(matched <= reference, s"Matched points must be less than reference points (got $matched and $reference)")
    if (reference != 0) matched.toDouble / reference else 0
  }
}