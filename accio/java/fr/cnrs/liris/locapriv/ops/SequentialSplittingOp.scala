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
  category = "transform",
  help = "Split traces sequentially, according to chronological order.")
case class SequentialSplittingOp(
  @Arg(help = "Percentage of events at which a trace begins")
  percentBegin: Int,
  @Arg(help = "Percentage of events at which a trace ends")
  percentEnd: Int,
  @Arg(help = "Whether to take the complement trace")
  complement: Boolean = false,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends TransformOp[Event] {

  require(percentBegin >= 0 && percentBegin <= 100, s"percentBegin must be between 0 and 100: $percentBegin")
  require(percentEnd >= 0 && percentEnd <= 100, s"percentEnd must be between 0 and 100: $percentEnd")
  require(percentBegin <= percentEnd, s"percentEnd must be greater than percentBegin: $percentEnd < $percentBegin")

  override protected def transform(key: String, trace: Iterable[Event]): Iterable[Event] = {
    val from = math.max(0, (percentBegin.toDouble * trace.size / 100).floor.toInt)
    val until = math.min(trace.size, (percentEnd.toDouble * trace.size / 100).ceil.toInt)
    if (complement) {
      trace.slice(0, from) ++ trace.slice(until, trace.size)
    } else {
      trace.slice(from, until)
    }
  }
}