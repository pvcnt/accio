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

package fr.cnrs.liris.accio.ops

import fr.cnrs.liris.accio.framework.sdk.{Dataset, _}
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.accio.ops.model.{Event, Trace}

@Op(
  category = "transform",
  help = "Split traces, when there is a too huge distance between consecutive events.",
  cpu = 4,
  ram = "2G")
class SpatialGapSplittingOp extends Operator[SpatialGapSplittingIn, SpatialGapSplittingOut] with SlidingSplitting with SparkleOperator {
  override def execute(in: SpatialGapSplittingIn, ctx: OpContext): SpatialGapSplittingOut = {
    val split = (buffer: Seq[Event], curr: Event) => buffer.last.point.distance(curr.point) >= in.distance
    val output = read[Trace](in.data).flatMap(transform(_, split))
    SpatialGapSplittingOut(write(output, ctx))
  }
}

case class SpatialGapSplittingIn(
  @Arg(help = "Maximum distance between two consecutive events") distance: Distance,
  @Arg(help = "Input dataset") data: Dataset)

case class SpatialGapSplittingOut(
  @Arg(help = "Output dataset") data: Dataset)
