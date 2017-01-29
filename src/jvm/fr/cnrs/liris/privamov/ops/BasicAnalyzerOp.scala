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
import fr.cnrs.liris.privamov.core.model.Trace

@Op(
  help = "Compute basic statistics about traces.",
  category = "metric")
class BasicAnalyzerOp extends Operator[BasicAnalyzerIn, BasicAnalyzerOut] {
  override def execute(in: BasicAnalyzerIn, ctx: OpContext): BasicAnalyzerOut = {
    val data = ctx.read[Trace](in.data)
    val metrics = data.map { trace =>
      trace.id -> (trace.size, trace.length.meters, trace.duration.seconds)
    }.toArray
    BasicAnalyzerOut(
      size = metrics.map { case (k, v) => k -> v._1.toLong }.toMap,
      length = metrics.map { case (k, v) => k -> v._2 }.toMap,
      duration = metrics.map { case (k, v) => k -> v._3 }.toMap)
  }
}

case class BasicAnalyzerIn(@Arg(help = "Input dataset") data: Dataset)

case class BasicAnalyzerOut(
  @Arg(help = "Trace size") size: Map[String, Long],
  @Arg(help = "Trace length") length: Map[String, Double],
  @Arg(help = "Trace duration") duration: Map[String, Long])