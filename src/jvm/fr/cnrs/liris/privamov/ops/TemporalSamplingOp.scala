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

package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.accio.framework.sdk._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.privamov.core.model.{Event, Trace}

@Op(
  category = "transform",
  help = "Enforce a minimum duration between two consecutive events in traces.",
  description = "If the duration is less than a given threshold, events will be discarded until the next point " +
    "that fulfills the minimum duration requirement.",
  cpu = 4,
  ram = "2G")
class TemporalSamplingOp extends Operator[TemporalSamplingIn, TemporalSamplingOut] with SlidingSampling with SparkleOperator {
  override def execute(in: TemporalSamplingIn, ctx: OpContext): TemporalSamplingOut = {
    val sample = (prev: Event, curr: Event) => (prev.time to curr.time).duration >= in.duration
    val output = read[Trace](in.data).map(transform(_, sample))
    TemporalSamplingOut(write(output, ctx))
  }
}

case class TemporalSamplingIn(
  @Arg(help = "Minimum duration between two consecutive events") duration: org.joda.time.Duration,
  @Arg(help = "Input dataset") data: Dataset)

case class TemporalSamplingOut(
  @Arg(help = "Output dataset") data: Dataset)