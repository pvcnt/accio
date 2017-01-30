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
import fr.cnrs.liris.accio.agent.{ParseRunRequest, ParseRunResponse}
import fr.cnrs.liris.accio.core.domain.{InvalidSpecException, InvalidSpecMessage}
import fr.cnrs.liris.accio.core.dsl.RunParser

import scala.collection.mutable

/**
 * Parse a content written using run DSL into a run specification. It returns either a valid run specification,
 * possibly with some warnings, or a list of errors that the parser raised.
 *
 * @param runParser Run parser.
 */
final class ParseRunHandler @Inject()(runParser: RunParser) extends Handler[ParseRunRequest, ParseRunResponse] {
  override def handle(req: ParseRunRequest): Future[ParseRunResponse] = {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    try {
      val run = runParser.parse(req.content, req.params.toMap, warnings)
      Future(ParseRunResponse(run = Some(run), warnings = warnings.toSeq))
    } catch {
      case e: InvalidSpecException =>
        Future(ParseRunResponse(errors = e.errors, warnings = e.warnings))
    }
  }
}