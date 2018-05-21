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

package fr.cnrs.liris.accio.cli.commands

import com.twitter.util.Future
import fr.cnrs.liris.accio.server._

class GetOperatorController extends GetController[ListOperatorsResponse] {
  override def retrieve(opts: GetQuery, client: AgentService.MethodPerEndpoint): Future[ListOperatorsResponse] = {
    client.listOperators(ListOperatorsRequest(includeDeprecated = opts.all))
  }

  override def print(reporter: Reporter, resp: ListOperatorsResponse): Unit = {
    val maxLength = resp.operators.map(_.name.length).max
    resp.operators.sortBy(_.name).groupBy(_.category).foreach { case (category, categoryOps) =>
      reporter.outErr.printOutLn(s"Operators in $category category")
      categoryOps.foreach { op =>
        val padding = " " * (maxLength - op.name.length)
        reporter.outErr.printOutLn(s"  ${op.name}$padding ${op.help.getOrElse("")}")
      }
      reporter.outErr.printOutLn()
    }
  }
}