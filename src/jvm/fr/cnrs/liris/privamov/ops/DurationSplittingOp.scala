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
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.privamov.core.model.{Event, Trace}

@Op(
  category = "transform",
  help = "Split traces, ensuring a maximum duration for each one.",
  cpu = 4,
  ram = "2G")
class DurationSplittingOp extends Operator[DurationSplittingIn, DurationSplittingOut] with SlidingSplitting {
  override def execute(in: DurationSplittingIn, ctx: OpContext): DurationSplittingOut = {
    val split = (buffer: Seq[Event], curr: Event) => (buffer.head.time to curr.time).duration >= in.duration
    val output = ctx.read[Trace](in.data).flatMap(transform(_, split))
    DurationSplittingOut(ctx.write(output))
  }
}

case class DurationSplittingIn(
  @Arg(help = "Maximum duration of each trace") duration: org.joda.time.Duration,
  @Arg(help = "Input dataset") data: Dataset)

case class DurationSplittingOut(
  @Arg(help = "Output dataset") data: Dataset)
