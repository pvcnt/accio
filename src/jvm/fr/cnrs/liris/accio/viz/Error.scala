/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.viz

import com.fasterxml.jackson.annotation.JsonProperty

case class Error private(@JsonProperty("type") kind: String, message: Option[String])

object Error {
  def invalidRequest(message: String): Error = Error("invalid_request", Some(message))

  def invalidRequest: Error = Error("invalid_request", None)

  def api(message: String): Error = Error("api_error", Some(message))

  def api: Error = Error("api_error", None)

  def authentication(message: String): Error = Error("authentication_error", Some(message))

  def authentication: Error = Error("authentication_error", None)

  def rateLimit(message: String): Error = Error("rate_limit_error", Some(message))

  def rateLimit: Error = Error("rate_limit_error", None)
}