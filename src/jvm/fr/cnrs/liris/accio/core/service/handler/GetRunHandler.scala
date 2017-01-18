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
import fr.cnrs.liris.accio.core.domain.RunRepository

/**
 * Handler retrieving a single run, if it exists.
 *
 * @param repository Run repository.
 */
class GetRunHandler @Inject()(repository: RunRepository) extends Handler[GetRunRequest, GetRunResponse] {
  override def handle(req: GetRunRequest): Future[GetRunResponse] = {
    val maybeRun = repository.get(req.id)
    Future(GetRunResponse(maybeRun))
  }
}
