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

import fr.cnrs.liris.accio.core.api._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.privamov.core.model.Trace

@Op(
  category = "prepare",
  help = "Enforce a given size on each trace.",
  description = "Larger traces will be truncated, smaller traces will be discarded.",
  cpu = 4,
  ram = "2G")
class EnforceSizeOp  extends Operator[EnforceSizeIn, EnforceSizeOut] {
  override def execute(in: EnforceSizeIn, ctx: OpContext): EnforceSizeOut = {
    val data = ctx.read[Trace](in.data)
    val output = ctx.write(data.flatMap(transform(_, in.minSize, in.maxSize)))
    EnforceSizeOut(output)
  }

  private def transform(trace: Trace, minSize: Option[Int], maxSize: Option[Int]): Seq[Trace] = {
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

case class EnforceSizeIn(
  @Arg(help = "Minimum number of events in each trace") minSize: Option[Int],
  @Arg(help = "Maximum number of events in each trace") maxSize: Option[Int],
  @Arg(help = "Input dataset") data: Dataset)

case class EnforceSizeOut(
  @Arg(help = "Output dataset") data: Dataset)