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

package fr.cnrs.liris.accio.agent.handler

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{ListRunsRequest, ListRunsResponse}
import fr.cnrs.liris.accio.framework.storage.{RunQuery, Storage}

/**
 * Handler retrieving runs matching some search criteria.
 *
 * @param storage Storage.
 */
class ListRunsHandler @Inject()(storage: Storage) extends AbstractHandler[ListRunsRequest, ListRunsResponse] {
  override def handle(req: ListRunsRequest): Future[ListRunsResponse] = {
    val query = RunQuery(
      name = req.name,
      owner = req.owner,
      workflow = req.workflowId,
      status = req.status.toSet,
      parent = req.parent,
      clonedFrom = req.clonedFrom,
      tags = req.tags.toSet,
      q = req.q,
      limit = req.limit,
      offset = req.offset)
    val res = storage.read(_.runs.find(query))
    Future(ListRunsResponse(res.results, res.totalCount))
  }
}