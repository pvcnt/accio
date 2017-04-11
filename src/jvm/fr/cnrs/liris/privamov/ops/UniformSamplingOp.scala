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

import fr.cnrs.liris.accio.core.operator._
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.common.random.RandomUtils
import fr.cnrs.liris.privamov.core.model.Trace

import scala.util.Random

@Op(
  category = "transform",
  help = "Uniformly sample events inside traces.",
  description = "Perform a uniform sampling on traces, keeping each event with a given probability.",
  unstable = true,
  cpu = 4,
  ram = "2G")
class UniformSamplingOp extends Operator[UniformSamplingIn, UniformSamplingOut] {

  override def execute(in: UniformSamplingIn, ctx: OpContext): UniformSamplingOut = {
    val input = ctx.read[Trace](in.data)
    val rnd = new Random(ctx.seed)
    val seeds = input.keys.map(key => key -> rnd.nextLong()).toMap
    val output = input.map(trace => transform(trace, in.probability, seeds(trace.id)))
    UniformSamplingOut(ctx.write(output))
  }

  private def transform(trace: Trace, probability: Double, seed: Long): Trace = {
    trace.replace(RandomUtils.sampleUniform(trace.events, probability, seed))
  }
}

case class UniformSamplingIn(
  @Arg(help = "Probability to keep each event") probability: Double,
  @Arg(help = "Input dataset") data: Dataset) {
  require(probability >= 0 && probability <= 1, s"Probability must be in [0, 1] (got $probability)")
}

case class UniformSamplingOut(
  @Arg(help = "Output dataset") data: Dataset)
