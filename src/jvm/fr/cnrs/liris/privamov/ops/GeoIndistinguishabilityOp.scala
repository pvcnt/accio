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
import fr.cnrs.liris.privamov.core.lppm.Laplace
import fr.cnrs.liris.privamov.core.model.Trace

import scala.util.Random

@Op(
  category = "lppm",
  help = "Enforce geo-indistinguishability guarantees on traces.",
  description = "Generate locations satisfying geo-indistinguishability properties. The method used here is the one " +
    "presented by the authors of the paper and consists in adding noise following a double-exponential distribution.",
  unstable = true,
  cpu = 4,
  ram = "2G")
class GeoIndistinguishabilityOp extends Operator[GeoIndistinguishabilityIn, GeoIndistinguishabilityOut] {
  override def execute(in: GeoIndistinguishabilityIn, ctx: OpContext): GeoIndistinguishabilityOut = {
    val input = ctx.read[Trace](in.data)
    val rnd = new Random(ctx.seed)
    val seeds = input.keys.map(key => key -> rnd.nextLong()).toMap
    val output = input.map(trace => new Laplace(in.epsilon, seeds(trace.id)).transform(trace))
    GeoIndistinguishabilityOut(ctx.write(output))
  }
}

case class GeoIndistinguishabilityIn(
  @Arg(help = "Privacy budget") epsilon: Double = 0.001,
  @Arg(help = "Input dataset") data: Dataset) {
  require(epsilon > 0, s"Epsilon must be strictly positive (got $epsilon)")
}

case class GeoIndistinguishabilityOut(
  @Arg(help = "Output dataset") data: Dataset)