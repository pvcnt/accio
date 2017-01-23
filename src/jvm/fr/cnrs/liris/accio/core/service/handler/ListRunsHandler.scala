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

package fr.cnrs.liris.accio.core.service.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.core.domain.{ReadOnlyRunRepository, RunQuery}

/**
 * Handler retrieving runs matching some search criteria. It does *not* return the result of each node, only
 * its state.
 *
 * @param repository Run repository (read-only).
 */
class ListRunsHandler @Inject()(repository: ReadOnlyRunRepository) extends Handler[ListRunsRequest, ListRunsResponse] {
  override def handle(req: ListRunsRequest): Future[ListRunsResponse] = {
    val query = RunQuery(
      name = req.name,
      owner = req.owner,
      workflow = req.workflowId,
      status = req.status.toSet.flatten,
      limit = req.limit.getOrElse(25),
      offset = req.offset)
    val res = repository.find(query)
    val runs = res.results.map(run => run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult))))
    Future(ListRunsResponse(runs, res.totalCount))
  }
}