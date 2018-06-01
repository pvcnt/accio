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
  help = "Regularly sample events inside traces using the modulo operator.",
  description = "It will ensure that the final number of events is exactly (+/- 1) the one " +
    "required, and that events are regularly sampled (i.e., one out of x).")
case class ModuloSamplingOp(
  @Arg(help = "Number of events to keep")
  n: Int,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends TransformOp[Event] {

  require(n >= 0, s"n must be strictly positive: $n")

  override protected def transform(key: String, trace: Iterable[Event]): Iterable[Event] = {
    if (trace.size <= n) {
      trace
    } else {
      val modulo = trace.size.toDouble / n
      // We add an additional take(n) just in case some floating point operation gave an inadequate
      // result, but it is theoretically unnecessary.
      trace.zipWithIndex.filter { case (_, idx) => (idx % modulo) < 1 }.map(_._1).take(n)
    }
  }
}
