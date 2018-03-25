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

package fr.cnrs.liris.accio.runtime.commandbus

import com.twitter.util.Future

import scala.reflect._

/**
 * Interface for handlers, converting a request into a response.
 *
 * @tparam Req Request type.
 * @tparam Res Response type.
 */
trait Handler[Req, Res] {
  def requestClass: Class[Req]

  def handle(req: Req): Future[Res]
}

abstract class AbstractHandler[Req: ClassTag, Res: ClassTag] extends Handler[Req, Res] {
  override final def requestClass: Class[Req] = classTag[Req].runtimeClass.asInstanceOf[Class[Req]]
}