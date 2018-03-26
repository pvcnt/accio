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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.locapriv.model.Trace

@Op(
  category = "metric",
  help = "Compute transmission delay between two datasets of traces",
  cpu = 4,
  ram = "2G")
class TransmissionDelayOp extends Operator[TransmissionDelayIn, TransmissionDelayOut] with SparkleOperator {

  override def execute(in: TransmissionDelayIn, ctx: OpContext): TransmissionDelayOut = {
    val train = read[Trace](in.train)
    val test = read[Trace](in.test)
    val values = train.zip(test).map { case (ref, res) => evaluate(ref, res) }.toArray
    TransmissionDelayOut(values.toMap)
  }

  private def evaluate(ref: Trace, res: Trace) = {
    requireState(ref.id == res.id, s"Trace mismatch: ${ref.id} / ${res.id}")
    res.id -> (ref.events.last.time to res.events.last.time).millis
  }
}

case class TransmissionDelayIn(
  @Arg(help = "Train dataset") train: Dataset,
  @Arg(help = "Test dataset") test: Dataset)

case class TransmissionDelayOut(
  @Arg(help = "Transmission delay") value: Map[String, Long])