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

package fr.cnrs.liris.infra.thriftserver

import com.twitter.finagle.context.Contexts
import fr.cnrs.liris.util.StringUtils.maybe

case class UserInfo(name: String, email: Option[String] = None, groups: Set[String] = Set.empty)

object UserInfo {
  private[this] val key = new Contexts.local.Key[UserInfo]()

  val Anonymous = UserInfo("system:anonymous", groups = Set("system:unauthenticated"))

  def current: Option[UserInfo] = Contexts.local.get(key)

  def let[R](userInfo: UserInfo)(f: => R): R = Contexts.local.let(key, userInfo)(f)

  def parse(str: String): UserInfo = {
    str.split(':').take(3) match {
      case Array(name) => UserInfo(name)
      case Array(name, email) => UserInfo(name, maybe(email))
      case Array(name, email, groups) =>
        UserInfo(name, maybe(email), groups.split(',').filter(_.nonEmpty).toSet)
    }
  }
}