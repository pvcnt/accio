/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.privamov.core.io.{Decoder, Encoder}
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv
import org.joda.time.Instant

@Op(
  category = "prepare",
  help = "Collapse temporal gaps between days.",
  description = "Removes empty days by shifting data to fill those empty days.")
class CollapseTemporalGapsOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[CollapseTemporalGapsIn, CollapseTemporalGapsOut] with SparkleOperator {

  override def execute(in: CollapseTemporalGapsIn, ctx: OpContext): CollapseTemporalGapsOut = {
    val startAt = new Instant(in.startAt.millis).toDateTime.withTimeAtStartOfDay
    val input = read[Trace](in.data)
    val output = write(input.map(transform(_, startAt)), ctx.sandboxDir)
    CollapseTemporalGapsOut(output)
  }

  private def transform(trace: Trace, startAt: DateTime) = {
    trace.replace { events =>
      var shift = 0L
      var prev: Option[DateTime] = None
      events.map { event =>
        val time = event.time.toDateTime.withTimeAtStartOfDay
        if (prev.isEmpty) {
          shift = (time to startAt).duration.days
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
