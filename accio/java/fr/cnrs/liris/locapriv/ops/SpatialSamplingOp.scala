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
import fr.cnrs.liris.util.geo.Distance

@Op(
  category = "transform",
  help = "Enforce a minimum distance between two consecutive events in traces.",
  description = "If the distance is less than a given threshold, records will be discarded until " +
    "the next point that fulfills the minimum distance requirement.")
case class SpatialSamplingOp(
  @Arg(help = "Minimum distance between two consecutive events")
  distance: Distance,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends SlidingSamplingOp {

  override protected def sample(prev: Event, curr: Event): Boolean = {
    prev.point.distance(curr.point) >= distance
  }
}