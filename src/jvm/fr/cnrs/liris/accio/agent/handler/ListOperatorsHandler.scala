/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.agent.handler.api

import com.google.inject.Inject
import com.twitter.util.Future
import fr.cnrs.liris.accio.runtime.commandbus.AbstractHandler
import fr.cnrs.liris.accio.agent.{ListOperatorsRequest, ListOperatorsResponse}
import fr.cnrs.liris.accio.framework.service.OpRegistry

/**
 * Retrieve all known operators.
 *
 * @param opRegistry Operator registry.
 */
class ListOperatorsHandler @Inject()(opRegistry: OpRegistry)
  extends AbstractHandler[ListOperatorsRequest, ListOperatorsResponse] {

    override def handle(req: ListOperatorsRequest): Future[ListOperatorsResponse] = {
      var results = opRegistry.ops.toSeq
      if (!req.includeDeprecated) {
        results = results.filter(_.deprecation.isEmpty)
      }
      Future(ListOperatorsResponse(results))
    }
  }