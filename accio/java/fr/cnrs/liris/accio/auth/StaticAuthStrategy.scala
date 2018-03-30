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

package fr.cnrs.liris.accio.auth

import java.nio.file.Path

import com.twitter.util.Future
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.UserInfo
import fr.cnrs.liris.common.util.StringUtils.maybe

import scala.io.Source

final class StaticAuthStrategy(users: Set[StaticAuthStrategy.AuthInfo]) extends AuthStrategy {
  private[this] val index = users.map(user => user.clientId -> user).toMap

  override def authenticate(clientId: String): Future[Option[UserInfo]] = Future {
    index.get(clientId) match {
      case Some(authInfo) => Some(authInfo.user)
      case None => None
    }
  }
}

object StaticAuthStrategy extends Logging {

  private case class AuthInfo(clientId: String, user: UserInfo)

  /**
   * Create a [[StaticAuthStrategy]] from a configuration file.
   *
   * @param path Path to the users' database.
   * @throws IllegalArgumentException If the provided path is not a readable file.
   */
  def fromFile(path: Path): StaticAuthStrategy = {
    if (!path.toFile.isFile || !path.toFile.canRead) {
      throw new IllegalArgumentException(s"$path must be a readable file")
    }
    val lines = Source.fromFile(path.toFile).getLines()
    val users = lines.zipWithIndex.flatMap { case (line, idx) => parse(idx, line) }.toSet
    new StaticAuthStrategy(users)
  }

  private def parse(idx: Int, line: String): Option[AuthInfo] = {
    line.split(':') match {
      case Array(clientId, name) => Some(AuthInfo(clientId, UserInfo(name)))
      case Array(clientId, name, email) =>
        Some(AuthInfo(clientId, UserInfo(name, maybe(email))))
      case Array(clientId, name, email, groups) =>
        Some(AuthInfo(clientId, UserInfo(name, maybe(email), groups.split(',').filter(_.nonEmpty).toSet)))
      case _ =>
        logger.warn(s"Ignored weird line $idx: $line")
        None
    }
  }
}