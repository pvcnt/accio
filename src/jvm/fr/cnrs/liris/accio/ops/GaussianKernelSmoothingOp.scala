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

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.framework.sdk.{Dataset, _}
import fr.cnrs.liris.common.geo.Point
import fr.cnrs.liris.accio.ops.model.{Event, Trace}

@Op(
  category = "transform",
  help = "Apply gaussian kernel smoothing on traces.",
  description = "Apply gaussian kernel smoothing on a trace, attenuating the impact of noisy observations.",
  cpu = 4,
  ram = "2G")
class GaussianKernelSmoothingOp extends Operator[GaussianKernelSmoothingIn, GaussianKernelSmoothingOut] with SparkleOperator {
  override def execute(in: GaussianKernelSmoothingIn, ctx: OpContext): GaussianKernelSmoothingOut = {
    val data = read[Trace](in.data)
    val output = write(data.map(transform(_, in.omega)), ctx)
    GaussianKernelSmoothingOut(output)
  }

  private def transform(trace: Trace, omega: Duration): Trace = trace.replace(_.map(transform(_, trace, omega)))

  private def transform(event: Event, trace: Trace, omega: Duration) = {
    var ks = 0d
    var x = 0d
    var y = 0d
    for (i <- trace.events.indices) {
      val k = gaussianKernel(event.time.millis, trace.events(i).time.millis, omega)
      ks += k
      x += k * trace.events(i).point.x
      y += k * trace.events(i).point.y
    }
    x /= ks
    y /= ks
    event.copy(point = Point(x, y))
  }

  private def gaussianKernel(t1: Long, t2: Long, omega: Duration): Double =
    Math.exp(-Math.pow(t1 - t2, 2) / (2 * omega.millis * omega.millis))
}

case class GaussianKernelSmoothingIn(
  @Arg(help = "Bandwidth") omega: org.joda.time.Duration,
  @Arg(help = "Input dataset") data: Dataset)

case class GaussianKernelSmoothingOut(@Arg(help = "Output dataset") data: Dataset)