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

import fr.cnrs.liris.accio.sdk.{Arg, OpContext, ScalaOperator}
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.sparkle.Encoder

import scala.util.Random

private[ops] abstract class TransformOp[T: Encoder]
  extends ScalaOperator[TransformOp.Out] with SparkleOperator {
  this: Product =>

  protected var seeds = Map.empty[String, Long]

  override final def execute(ctx: OpContext): TransformOp.Out = {
    val input = read[Event](data)
    val traces = input.groupBy(_.id)
    if (ctx.hasSeed) {
      val rnd = new Random(ctx.seed)
      seeds = traces.map { case (key, _) => key -> rnd.nextLong() }.collect().toMap
    }
    val output = traces.flatMap { case (key, trace) => transform(key, trace) }
    TransformOp.Out(write(output, 0, ctx))
  }

  protected def data: RemoteFile

  protected def transform(key: String, trace: Iterable[Event]): Iterable[T]
}

object TransformOp {

  case class Out(
    @Arg(help = "Transformed dataset")
    data: RemoteFile)

}