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

package fr.cnrs.liris.accio.tools.cli.controller

import com.twitter.util.Future
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.tools.cli.event.Reporter
import fr.cnrs.liris.util.StringUtils.padTo

case class GetQuery(all: Boolean, tags: Set[String], owner: Option[String], limit: Option[Int])

trait GetController[Res] {
  def retrieve(opts: GetQuery, client: AgentService.MethodPerEndpoint): Future[Res]

  def print(reporter: Reporter, resp: Res): Unit
}

private[controller] abstract class AbstractGetController[Res] extends GetController[Res] {
  override final def print(reporter: Reporter, resp: Res): Unit = {
    columns.zipWithIndex.foreach { case ((name, width), idx) =>
      if (idx > 0) {
        reporter.outErr.printOut("  ")
      }
      reporter.outErr.printOut(padTo(name.toUpperCase, width))
    }
    reporter.outErr.printOutLn()
    rows(resp).foreach { row =>
      columns.zipWithIndex.foreach { case ((_, width), idx) =>
        if (idx > 0) {
          reporter.outErr.printOut("  ")
        }
        reporter.outErr.printOut(padTo(row(idx).toString, width))
      }
      reporter.outErr.printOutLn()
    }
    val more = moreRows(resp)
    if (more > 0) {
      reporter.outErr.printOutLn(s"$more more...")
    }
  }

  protected def columns: Seq[(String, Int)]

  protected def rows(resp: Res): Seq[Seq[Any]]

  protected def moreRows(resp: Res): Int = 0
}