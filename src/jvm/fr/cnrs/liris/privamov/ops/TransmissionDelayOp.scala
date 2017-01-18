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

import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.util.Requirements._
import fr.cnrs.liris.privamov.core.io.Decoder
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "metric",
  help = "Compute transmission delay between two datasets of traces")
class TransmissionDelayOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]])
  extends Operator[TransmissionDelayIn, TransmissionDelayOut] with SparkleReadOperator {

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