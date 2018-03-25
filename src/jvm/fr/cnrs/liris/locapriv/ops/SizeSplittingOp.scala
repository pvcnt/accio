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
import fr.cnrs.liris.locapriv.model.{Event, Trace}

@Op(
  category = "transform",
  help = "Split traces, ensuring a maximum size for each one.",
  cpu = 4,
  ram = "2G")
class SizeSplittingOp extends Operator[SizeSplittingIn, SizeSplittingOut] with SlidingSplitting with SparkleOperator {
  override def execute(in: SizeSplittingIn, ctx: OpContext): SizeSplittingOut = {
    val split = (buffer: Seq[Event], curr: Event) => buffer.size >= in.size
    val output = read[Trace](in.data).flatMap(transform(_, split))
    SizeSplittingOut(write(output, ctx))
  }
}

case class SizeSplittingIn(
  @Arg(help = "Maximum number of events allowed in each trace") size: Int,
  @Arg(help = "Input dataset") data: Dataset)

case class SizeSplittingOut(
  @Arg(help = "Output dataset") data: Dataset)