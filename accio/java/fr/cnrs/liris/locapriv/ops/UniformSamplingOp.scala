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

import fr.cnrs.liris.accio.sdk._
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.util.random.RandomUtils

@Op(
  category = "transform",
  help = "Uniformly sample events inside traces.",
  description = "Perform a uniform sampling on traces, keeping each event with a given probability.",
  unstable = true,
  cpus = 4,
  ram = "2G")
case class UniformSamplingOp(
  @Arg(help = "Probability to keep each event")
  probability: Double,
  @Arg(help = "Input dataset")
  data: RemoteFile)
  extends TransformOp[Event] {

  require(probability >= 0 && probability <= 1, s"Probability must be in [0, 1] (got $probability)")

  override protected def transform(key: String, trace: Seq[Event]): Seq[Event] = {
    RandomUtils.sampleUniform(trace, probability, seeds(key))
  }
}