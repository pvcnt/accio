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

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.privamov.core.io.{Decoder, Encoder}
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "prepare",
  help = "Split traces sequentially, according to chronological order.")
class SequentialSplittingOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[SequentialSplittingIn, SequentialSplittingOut] with SparkleOperator {

  override def execute(in: SequentialSplittingIn, ctx: OpContext): SequentialSplittingOut = {
    val output = read[Trace](in.data).map(transform(_, in.percentBegin, in.percentEnd, in.complement))
    SequentialSplittingOut(write(output, ctx.workDir))
  }

  private def transform(trace: Trace, percentBegin: Double, percentEnd: Double, complement: Boolean): Trace = {
    val from = math.max(0, (percentBegin * trace.size / 100).floor.toInt)
    val until = math.min(trace.size, (percentEnd * trace.size / 100).ceil.toInt)
    if (complement) {
      trace.replace(events => events.slice(0, from) ++ events.slice(until, events.size))
    } else {
      trace.replace(_.slice(from, until))
    }
  }
}

case class SequentialSplittingIn(
  @Arg(help = "Percentage of events at which a trace begins") percentBegin: Double,
  @Arg(help = "Percentage of events at which a trace ends") percentEnd: Double,
  @Arg(help = "Whether to take the complement trace") complement: Boolean = false,
  @Arg(help = "Input dataset") data: Dataset) {
  require(percentBegin >= 0 && percentBegin <= 100, s"Begin percentage must be in [0,100] (got $percentBegin)")
  require(percentEnd >= 0 && percentEnd <= 100, s"End percentage must be in [0,100] (got $percentEnd)")
  require(percentBegin <= percentEnd, s"End percentage must be greater than begin percentage")
}

case class SequentialSplittingOut(
  @Arg(help = "Output dataset") data: Dataset)