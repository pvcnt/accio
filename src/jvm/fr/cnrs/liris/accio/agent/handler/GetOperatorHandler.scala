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
import fr.cnrs.liris.accio.agent.{GetOperatorRequest, GetOperatorResponse}
import fr.cnrs.liris.accio.framework.service.OpRegistry

/**
 * Retrieve a single operator, if it exists.
 *
 * @param opRegistry Operator registry.
 */
final class GetOperatorHandler @Inject()(opRegistry: OpRegistry)
  extends AbstractHandler[GetOperatorRequest, GetOperatorResponse] {

    override def handle(req: GetOperatorRequest): Future[GetOperatorResponse] = {
      val maybeOpDef = opRegistry.get(req.name)
      Future(GetOperatorResponse(maybeOpDef))
    }
  }