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

import fr.cnrs.liris.accio.sdk.{RemoteFile, _}
import fr.cnrs.liris.locapriv.domain.Trace

@Op(
  category = "transform",
  help = "Split traces sequentially, according to chronological order.",
  cpus = 4,
  ram = "2G")
case class SequentialSplittingOp(
  @Arg(help = "Percentage of events at which a trace begins")
  percentBegin: Double,
  @Arg(help = "Percentage of events at which a trace ends")
  percentEnd: Double,
  @Arg(help = "Whether to take the complement trace")
  complement: Boolean = false,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends ScalaOperator[SequentialSplittingOut] with SparkleOperator {
  require(percentBegin >= 0 && percentBegin <= 100, s"Begin percentage must be in [0,100] (got $percentBegin)")
  require(percentEnd >= 0 && percentEnd <= 100, s"End percentage must be in [0,100] (got $percentEnd)")
  require(percentBegin <= percentEnd, s"End percentage must be greater than begin percentage")

  override def execute(ctx: OpContext): SequentialSplittingOut = {
    val output = read[Trace](data).map(transform)
    SequentialSplittingOut(write(output, ctx))
  }

  private def transform(trace: Trace): Trace = {
    val from = math.max(0, (percentBegin * trace.size / 100).floor.toInt)
    val until = math.min(trace.size, (percentEnd * trace.size / 100).ceil.toInt)
    if (complement) {
      trace.replace(events => events.slice(0, from) ++ events.slice(until, events.size))
    } else {
      trace.replace(_.slice(from, until))
    }
  }
}

case class SequentialSplittingOut(
  @Arg(help = "Output dataset")
  data: RemoteFile)