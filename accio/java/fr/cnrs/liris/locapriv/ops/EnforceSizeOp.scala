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

import fr.cnrs.liris.accio.sdk.{Dataset, _}
import fr.cnrs.liris.locapriv.domain.Trace

@Op(
  category = "transform",
  help = "Enforce a given size on each trace.",
  description = "Larger traces will be truncated, smaller traces will be discarded.",
  cpus = 4,
  ram = "2G")
case class EnforceSizeOp(
  @Arg(help = "Minimum number of events in each trace")
  minSize: Option[Int],
  @Arg(help = "Maximum number of events in each trace")
  maxSize: Option[Int],
  @Arg(help = "Input dataset") data: Dataset)
  extends ScalaOperator[EnforceSizeOut] with SparkleOperator {

  override def execute(ctx: OpContext): EnforceSizeOut = {
    val output = write(read[Trace](data).flatMap(transform), ctx)
    EnforceSizeOut(output)
  }

  private def transform(trace: Trace): Seq[Trace] = {
    var res = trace
    maxSize.foreach { size =>
      if (res.size > size) {
        res = res.replace(_.take(size))
      }
    }
    minSize match {
      case None => Seq(res)
      case Some(size) => if (res.size < size) Seq.empty else Seq(res)
    }
  }
}

case class EnforceSizeOut(
  @Arg(help = "Output dataset") data: Dataset)