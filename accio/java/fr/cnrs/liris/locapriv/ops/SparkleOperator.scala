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

import fr.cnrs.liris.accio.sdk.{OpContext, RemoteFile, ScalaOperator}
import fr.cnrs.liris.sparkle.{DataFrame, Encoder, SparkleEnv}

private[ops] trait SparkleOperator {
  this: ScalaOperator[_] =>

  // Create a Sparkle environment using the numProcs' flag to limit parallelism.
  // It is a poor-man's way to isolate execution in terms of CPU usage.
  protected val env = new SparkleEnv(math.max(1, com.twitter.jvm.numProcs().round.toInt))

  protected final def read[T: Encoder](dataset: RemoteFile): DataFrame[T] = {
    env.read.csv(dataset.uri)
  }

  protected final def write[T](df: DataFrame[T], idx: Int, ctx: OpContext): RemoteFile = {
    val uri = ctx.workDir.resolve(idx.toString).toString
    df.write.csv(uri)
    RemoteFile(uri)
  }
}