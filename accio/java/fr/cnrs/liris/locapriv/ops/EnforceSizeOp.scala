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
  help = "Enforce a given size on each trace.",
  description = "Larger traces will be truncated, smaller traces will be discarded.")
case class EnforceSizeOp(
  @Arg(help = "Minimum number of events in each trace")
  minSize: Option[Int],
  @Arg(help = "Maximum number of events in each trace")
  maxSize: Option[Int],
  @Arg(help = "Input dataset") data: RemoteFile)
  extends TransformOp[Event] {

  override protected def transform(key: String, trace: Iterable[Event]): Iterable[Event] = {
    var result = trace
    maxSize.foreach { size =>
      if (result.size > size) {
        result = result.take(size)
      }
    }
    minSize match {
      case None => result
      case Some(size) => if (result.size < size) Seq.empty else result
    }
  }
}