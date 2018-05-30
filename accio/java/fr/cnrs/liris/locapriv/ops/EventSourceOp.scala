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
import fr.cnrs.liris.lumos.domain.RemoteFile

@Op(
  category = "source",
  help = "Read a dataset of traces.",
  description = "This operator can manipulate the source dataset, essentially to reduce its size, " +
    "through some basic preprocessing.")
case class EventSourceOp(
  @Arg(help = "Dataset URI")
  url: String)
  extends ScalaOperator[EventSourceOp.Out] with SparkleOperator {

  override def execute(ctx: OpContext): EventSourceOp.Out = {
    EventSourceOp.Out(RemoteFile(pathToUriString(url), Some("text/csv")))
  }
}

object EventSourceOp {

  case class Out(
    @Arg(help = "Source dataset")
    data: RemoteFile)

}