/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.privamov.ops

import com.github.nscala_time.time.Imports._
import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv
import org.joda.time.Instant

@Op(
  category = "prepare",
  help = "Collapse temporal gaps between days.",
  description = "Removes empty days by shifting data to fill those empty days.")
class CollapseTemporalGapsOp @Inject()(env: SparkleEnv) extends Operator[CollapseTemporalGapsIn, CollapseTemporalGapsOut] with SparkleOperator {

  override def execute(in: CollapseTemporalGapsIn, ctx: OpContext): CollapseTemporalGapsOut = {
    val startAt = new Instant(in.startAt.millis).toDateTime.withTimeAtStartOfDay
    val input = read(in.data, env)
    val output = write(input.map(transform(_, startAt)), ctx.workDir)
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
