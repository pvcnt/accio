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
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.privamov.core.model.Trace
import org.joda.time.Instant

@Op(
  category = "transform",
  help = "Collapse temporal gaps between days.",
  description = "Removes empty days by shifting data to fill those empty days.",
  cpu = 4,
  ram = "2G")
class CollapseTemporalGapsOp extends Operator[CollapseTemporalGapsIn, CollapseTemporalGapsOut] {
  override def execute(in: CollapseTemporalGapsIn, ctx: OpContext): CollapseTemporalGapsOut = {
    val startAt = new Instant(in.startAt.millis).toDateTime(DateTimeZone.UTC).withTimeAtStartOfDay
    val input = ctx.read[Trace](in.data)
    val output = ctx.write(input.map(transform(_, startAt)))
    CollapseTemporalGapsOut(output)
  }

  private def transform(trace: Trace, startAt: DateTime) = {
    trace.replace { events =>
      var shift = 0L
      var prev: Option[DateTime] = None
      events.map { event =>
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
}

case class CollapseTemporalGapsIn(
  @Arg(help = "Start date for all traces") startAt: Instant,
  @Arg(help = "Input dataset") data: Dataset)

case class CollapseTemporalGapsOut(
  @Arg(help = "Output dataset") data: Dataset)
