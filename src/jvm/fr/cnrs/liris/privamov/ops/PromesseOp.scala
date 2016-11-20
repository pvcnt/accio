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
import fr.cnrs.liris.common.geo.Distance
import fr.cnrs.liris.privamov.core.io.{Decoder, Encoder}
import fr.cnrs.liris.privamov.core.lppm.SpeedSmoothing
import fr.cnrs.liris.privamov.core.model.Trace
import fr.cnrs.liris.privamov.core.sparkle.SparkleEnv

@Op(
  category = "lppm",
  help = "Enforce speed smoothing guarantees on traces.")
class PromesseOp @Inject()(
  override protected val env: SparkleEnv,
  override protected val decoders: Set[Decoder[_]],
  override protected val encoders: Set[Encoder[_]])
  extends Operator[PromesseIn, PromesseOut] with SparkleOperator {

  override def execute(in: PromesseIn, ctx: OpContext): PromesseOut = {
    val lppm = new SpeedSmoothing(in.epsilon)
    val output = read[Trace](in.data).map(lppm.transform)
    PromesseOut(write(output, ctx.workDir))
  }
}

case class PromesseIn(
  @Arg(help = "Distance to enforce between two consecutive points") epsilon: Distance,
  @Arg(help = "Input dataset") data: Dataset)

case class PromesseOut(@Arg(help = "Output dataset") data: Dataset)