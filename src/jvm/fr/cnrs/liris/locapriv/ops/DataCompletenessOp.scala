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

import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.locapriv.model.Trace

@Op(
  category = "metric",
  help = "Compute data completeness difference between two datasets of traces.",
  cpu = 2,
  ram = "1G")
class DataCompletenessOp extends Operator[DataCompletenessIn, DataCompletenessOut] with SparkleOperator {
  override def execute(in: DataCompletenessIn, ctx: OpContext): DataCompletenessOut = {
    val train = read[Trace](in.train)
    val test = read[Trace](in.test)
    val values = train.zip(test).map { case (ref, res) => evaluate(ref, res) }.toArray
    DataCompletenessOut(values.toMap)
  }

  private def evaluate(ref: Trace, res: Trace) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    val value = {
      if (res.isEmpty && ref.isEmpty) 1d
      else if (res.isEmpty) 0d
      else res.size.toDouble / ref.size
    }
    res.id -> value
  }
}

case class DataCompletenessIn(
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class DataCompletenessOut(
  @Arg(help = "Data completeness") value: Map[String, Double])
