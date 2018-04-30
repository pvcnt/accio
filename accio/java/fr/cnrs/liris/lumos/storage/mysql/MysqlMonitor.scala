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

package fr.cnrs.liris.lumos.storage.mysql

import com.twitter.finagle.mysql.ServerError
import com.twitter.util.Monitor

private[mysql] object MysqlMonitor extends Monitor {
  private[this] val whitelist = Set(1062 /* Duplicate key */)

  override def handle(exc: Throwable): Boolean = {
    exc match {
      case s: ServerError => whitelist.contains(s.code)
      case _ => false
    }
  }
}