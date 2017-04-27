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

package fr.cnrs.liris.accio.tools.cli.controller

import com.twitter.util.Future
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.runtime.event.Reporter

class GetOperatorController extends GetController[ListOperatorsResponse] {
  override def retrieve(opts: GetQuery, client: AgentService$FinagleClient): Future[ListOperatorsResponse] = {
    client.listOperators(ListOperatorsRequest(includeDeprecated = opts.all))
  }

  override def print(reporter: Reporter, resp: ListOperatorsResponse): Unit = {
    val maxLength = resp.results.map(_.name.length).max
    resp.results.sortBy(_.name).groupBy(_.category).foreach { case (category, categoryOps) =>
      reporter.outErr.printOutLn(s"Operators in $category category")
      categoryOps.foreach { op =>
        val padding = " " * (maxLength - op.name.length)
        reporter.outErr.printOutLn(s"  ${op.name}$padding ${op.help.getOrElse("")}")
      }
      reporter.outErr.printOutLn()
    }
  }
}