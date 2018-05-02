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
import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Trace

@Op(
  category = "transform",
  help = "Enforce a given duration on each trace.",
  description = "Longer traces will be truncated, shorter traces will be discarded.",
  cpus = 4,
  ram = "2G")
case class EnforceDurationOp(
  @Arg(help = "Minimum duration of a trace")
  minDuration: Option[Duration],
  @Arg(help = "Maximum duration of a trace")
  maxDuration: Option[Duration],
  @Arg(help = "Input dataset") data: RemoteFile)
  extends ScalaOperator[EnforceDurationOut] with SparkleOperator {

  override def execute(ctx: OpContext): EnforceDurationOut = {
    val output = write(read[Trace](data).flatMap(transform), ctx)
    EnforceDurationOut(output)
  }

  private def transform(trace: Trace): Seq[Trace] = {
    var res = trace
    maxDuration.foreach { duration =>
      val startAt = res.events.head.time
      val endAt = startAt + duration
      res = res.replace(_.takeWhile(r => r.time <= endAt))
    }
    minDuration match {
      case None => Seq(res)
      case Some(duration) => if (res.duration < duration) Seq.empty else Seq(res)
    }
  }
}

case class EnforceDurationOut(
  @Arg(help = "Output dataset")
  data: RemoteFile)