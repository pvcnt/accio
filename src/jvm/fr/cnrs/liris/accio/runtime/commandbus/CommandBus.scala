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

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Future

@Singleton
class CommandBus @Inject()(handlers: Set[Handler[_, _]]) {
  @throws[NoHandlerException]
  @throws[MultipleHandlersException]
  def handle[T](req: T): Future[Any] = {
    val matchingHandlers = handlers.filter(_.requestClass.isAssignableFrom(req.getClass))
    if (matchingHandlers.isEmpty) {
      throw new NoHandlerException(req.getClass)
    } else if (matchingHandlers.size > 1) {
      throw new MultipleHandlersException(req.getClass)
    }
    matchingHandlers.head.asInstanceOf[Handler[T, _]].handle(req)
  }
}

class NoHandlerException(val requestClass: Class[_])
  extends IllegalArgumentException(s"No handler configured for ${requestClass.getName}")

class MultipleHandlersException(val requestClass: Class[_])
  extends IllegalArgumentException(s"Multiple handlers configured for ${requestClass.getName}")