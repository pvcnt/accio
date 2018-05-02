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
import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.locapriv.domain.{Event, Trace}

@Op(
  category = "transform",
  help = "Split traces, ensuring a maximum duration for each one.",
  cpus = 4,
  ram = "2G")
case class DurationSplittingOp(
  @Arg(help = "Maximum duration of each trace") duration: org.joda.time.Duration,
  @Arg(help = "Input dataset") data: Dataset)
  extends ScalaOperator[DurationSplittingOut] with SlidingSplitting with SparkleOperator {

  override def execute(ctx: OpContext): DurationSplittingOut = {
    val split = (buffer: Seq[Event], curr: Event) => (buffer.head.time to curr.time).duration >= duration
    val output = read[Trace](data).flatMap(transform(_, split))
    DurationSplittingOut(write(output, ctx))
  }
}

case class DurationSplittingOut(
  @Arg(help = "Output dataset")
  data: Dataset)
