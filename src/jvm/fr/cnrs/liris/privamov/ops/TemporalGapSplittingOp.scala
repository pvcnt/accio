/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import fr.cnrs.liris.privamov.core.io.{Decoder, Encoder}
import fr.cnrs.liris.privamov.core.model.{Event, Trace}
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "prepare",
  help = "Split traces, when there is a too long duration between consecutive events.")
class TemporalGapSplittingOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[TemporalGapSplittingIn, TemporalGapSplittingOut] with SlidingSplitting with SparkleOperator {

  override def execute(in: TemporalGapSplittingIn, ctx: OpContext): TemporalGapSplittingOut = {
    val split = (buffer: Seq[Event], curr: Event) => (buffer.last.time to curr.time).duration >= in.duration
    val output = read[Trace](in.data).flatMap(transform(_, split))
    TemporalGapSplittingOut(write(output, ctx.sandboxDir))
  }
}

case class TemporalGapSplittingIn(
  @Arg(help = "Maximum duration between two consecutive events") duration: org.joda.time.Duration,
  @Arg(help = "Input dataset") data: Dataset)

case class TemporalGapSplittingOut(
  @Arg(help = "Output dataset") data: Dataset)
