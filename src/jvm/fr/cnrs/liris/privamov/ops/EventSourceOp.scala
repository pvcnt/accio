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

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.common.util.FileUtils
import fr.cnrs.liris.privamov.core.io._
import fr.cnrs.liris.privamov.core.sparkle._

@Op(
  category = "source",
  help = "Read a dataset of traces.",
  description = "This operator can manipulate the source dataset, essentially to reduce its size, through some basic preprocessing.")
class EventSourceOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[EventSourceIn, EventSourceOut] with SparkleOperator {

  override def execute(in: EventSourceIn, ctx: OpContext): EventSourceOut = {
    val source = in.kind match {
      case "csv" => new CsvSource(FileUtils.expand(in.url), new CsvTraceDecoder)
      case "cabspotting" => CabspottingSource(FileUtils.expand(in.url))
      case "geolife" => GeolifeSource(FileUtils.expand(in.url))
      case _ => throw new IllegalArgumentException(s"Unknown kind: ${in.kind}")
    }
    val output = if (willWrite(in)) {
      var data = env.read(source)
      if (in.users.nonEmpty) {
        data = data.restrict(in.users.toSet)
      }
      in.sample.foreach { sample =>
        data = data.sample(withReplacement = false, fraction = sample, seed = ctx.seed)
      }
      write(data, ctx.workDir)
    } else Dataset(in.url)
    EventSourceOut(output)
  }

  override def isUnstable(in: EventSourceIn): Boolean = in.sample.isDefined

  private def willWrite(in: EventSourceIn) = in.kind != "csv" || in.sample.nonEmpty || in.users.nonEmpty
}

case class EventSourceIn(
  @Arg(help = "Dataset URL") url: String,
  @Arg(help = "Kind of dataset") kind: String = "csv",
  @Arg(help = "Sampling ratio") sample: Option[Double],
  @Arg(help = "Users to include") users: Seq[String] = Seq.empty)

case class EventSourceOut(@Arg(help = "Source dataset") data: Dataset)