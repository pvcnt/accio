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
import com.twitter.util.{Future, Time}
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{ListLogsRequest, ListLogsResponse}
import fr.cnrs.liris.accio.framework.storage.{LogsQuery, Storage}

/**
 * Retrieve run logs matching some search criteria.
 *
 * @param storage Storage.
 */
class ListLogsHandler @Inject()(storage: Storage) extends AbstractHandler[ListLogsRequest, ListLogsResponse] {
  override def handle(req: ListLogsRequest): Future[ListLogsResponse] = {
    val query = LogsQuery(
      runId = req.runId,
      nodeName = req.nodeName,
      classifier = req.classifier,
      limit = req.limit,
      since = req.since.map(Time.fromMilliseconds))
    val results = storage.logs.find(query)
    Future(ListLogsResponse(results))
  }
}