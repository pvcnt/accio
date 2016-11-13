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

package fr.cnrs.liris.accio.core.framework

import com.fasterxml.jackson.annotation.JsonValue

/**
 * A user of Accio.
 *
 * @param name  Username
 * @param email Email address
 */
case class User(name: String, email: Option[String] = None) {
  @JsonValue
  override def toString: String = name + email.map(" <" + _ + ">").getOrElse("")
}

/**
 * Factory of [[User]].
 */
object User {
  private[this] val UserRegex = "(.+)<(.+)>".r

  /**
   * Return the default user, inferred from the shell user login. It has no email address
   */
  val Default = User(sys.props("user.name"))

  /**
   * Parse a string into a user.
   * If it includes an email address, it should have the following format: `User name <handle@domain.tld>`.
   *
   * @param str String to parse
   */
  def parse(str: String): User = str match {
    case UserRegex(name, email) => new User(name.trim, Some(email.trim))
    case _ => new User(str, None)
  }
}