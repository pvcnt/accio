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

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event

@Op(
  category = "metric",
  help = "Compute data completeness difference between two datasets of traces.")
case class DataCompletenessOp(
  @Arg(help = "Train dataset")
  train: RemoteFile,
  @Arg(help = "Test dataset")
  test: RemoteFile)
  extends ScalaOperator[DataCompletenessOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): DataCompletenessOp.Out = {
    val trainDs = read[Event](train).groupBy(_.id)
    val testDs = read[Event](test).groupBy(_.id)
    val metrics = trainDs.join(testDs)(evaluate)
    DataCompletenessOp.Out(write(metrics, 0, ctx))
  }

  private def evaluate(id: String, ref: Iterable[Event], res: Iterable[Event]) = {
    val completeness = {
      if (res.isEmpty && ref.isEmpty) {
        1d
      } else if (res.isEmpty) {
        0d
      } else {
        res.size.toDouble / ref.size
      }
    }
    Seq(DataCompletenessOp.Value(id, ref.size, res.size, completeness))
  }
}

object DataCompletenessOp {

  case class Value(id: String, trainSize: Int, testSize: Int, completeness: Double)

  case class Out(
    @Arg(help = "Data completeness")
    metrics: RemoteFile)

}