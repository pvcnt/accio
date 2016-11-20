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

@Op(
  category = "prepare",
  help = "Enforce a given duration on each trace.",
  description = "Longer traces will be truncated, shorter traces will be discarded.")
class EnforceDurationOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[EnforceDurationIn, EnforceDurationOut] with SparkleOperator {

  override def execute(in: EnforceDurationIn, ctx: OpContext): EnforceDurationOut = {
    val data = read[Trace](in.data)
    val output = write(data.flatMap(transform(_, in.minDuration, in.maxDuration)), ctx.workDir)
    EnforceDurationOut(output)
  }

  private def transform(trace: Trace, minDuration: Option[Duration], maxDuration: Option[Duration]): Seq[Trace] = {
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

case class EnforceDurationIn(
  @Arg(help = "Minimum duration of a trace") minDuration: Option[org.joda.time.Duration],
  @Arg(help = "Maximum duration of a trace") maxDuration: Option[org.joda.time.Duration],
  @Arg(help = "Input dataset") data: Dataset)

case class EnforceDurationOut(
  @Arg(help = "Output dataset") data: Dataset)