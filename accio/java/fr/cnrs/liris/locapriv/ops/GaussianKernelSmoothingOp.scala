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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.util.geo.Point

@Op(
  category = "transform",
  help = "Apply gaussian kernel smoothing on traces.",
  description = "Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.")
case class GaussianKernelSmoothingOp(
  @Arg(help = "Bandwidth")
  omega: Duration,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends TransformOp[Event] {

  override protected def transform(key: String, trace: Iterable[Event]): Iterable[Event] = {
    val traceAsSeq = trace.toSeq
    trace.map(transform(_, traceAsSeq))
  }

  private def transform(event: Event, trace: Seq[Event]): Event = {
    var ks = 0d
    var x = 0d
    var y = 0d
    for (i <- trace.indices) {
      val k = gaussianKernel(event.time.millis, trace(i).time.millis, omega)
      ks += k
      x += k * trace(i).point.x
      y += k * trace(i).point.y
    }
    x /= ks
    y /= ks
    event.withPoint(Point(x, y))
  }

  private def gaussianKernel(t1: Long, t2: Long, omega: Duration): Double = {
    Math.exp(-Math.pow(t1 - t2, 2) / (2 * omega.millis * omega.millis))
  }
}