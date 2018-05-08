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
import fr.cnrs.liris.locapriv.domain.Event
import org.joda.time.Instant

@Op(
  category = "transform",
  help = "Collapse temporal gaps between days.",
  description = "Removes empty days by shifting data to fill those empty days.",
  cpus = 4,
  ram = "2G")
case class CollapseTemporalGapsOp(
  @Arg(help = "Start date for all traces")
  startAt: Instant,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends ScalaOperator[CollapseTemporalGapsOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): CollapseTemporalGapsOp.Out = {
    val startAtDate = new Instant(startAt.millis).toDateTime(DateTimeZone.UTC).withTimeAtStartOfDay
    val input = read[Event](data)
    val output = input.mapPartitions(transform(_, startAtDate))
    CollapseTemporalGapsOp.Out(write(output, 0, ctx))
  }

  private def transform(trace: Seq[Event], startAt: DateTime) = {
    var shift = 0L
    var prev: Option[DateTime] = None
    trace.map { event =>
      val time = event.time.toDateTime(DateTimeZone.UTC).withTimeAtStartOfDay
      if (prev.isEmpty) {
        shift = (if (time.isBefore(startAt)) time to startAt else startAt to time).duration.days
      } else if (time != prev.get) {
        val days = (prev.get to time).duration.days
        shift += days - 1
      }
      val aligned = if (shift.intValue > 0) {
        event.time - Duration.standardDays(shift)
      } else {
        event.time + Duration.standardDays(-shift)
      }
      prev = Some(time)
      event.copy(time = aligned)
    }
  }
}

object CollapseTemporalGapsOp {

  case class Out(
    @Arg(help = "Output dataset")
    data: RemoteFile)

}
