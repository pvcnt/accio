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

package fr.cnrs.liris.accio.tools.gateway

import com.fasterxml.jackson.annotation.JsonProperty

case class HttpError private(@JsonProperty("type") kind: String, message: Option[String])

object HttpError {
  def invalidRequest(message: String): HttpError = HttpError("invalid_request", Some(message))

  def invalidRequest: HttpError = HttpError("invalid_request", None)

  def api(message: String): HttpError = HttpError("api_error", Some(message))

  def api: HttpError = HttpError("api_error", None)

  def authentication(message: String): HttpError = HttpError("authentication_error", Some(message))

  def authentication: HttpError = HttpError("authentication_error", None)

  def rateLimit(message: String): HttpError = HttpError("rate_limit_error", Some(message))

  def rateLimit: HttpError = HttpError("rate_limit_error", None)
}