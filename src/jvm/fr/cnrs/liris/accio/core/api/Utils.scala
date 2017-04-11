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

package fr.cnrs.liris.accio.core.api

import fr.cnrs.liris.dal.core.api.{Value, Values}

import scala.util.matching.Regex

/**
 * Various helpers.
 */
object Utils {
  /**
   * Pattern for valid operator names.
   */
  val OpPattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid operator names.
   */
  val OpRegex: Regex = ("^" + OpPattern + "$").r

  /**
   * Pattern for valid node names.
   */
  val NodePattern = "[A-Z][a-zA-Z0-9_]+"

  /**
   * Regex for valid node names.
   */
  val NodeRegex: Regex = ("^" + NodePattern + "$").r

  /**
   * Pattern for valid workflow identifiers.
   */
  val WorkflowPattern = "[a-zA-Z][a-zA-Z0-9_.-]+"

  /**
   * Regex for valid workflow identifiers.
   */
  val WorkflowRegex: Regex = ("^" + WorkflowPattern + "$").r

  /**
   * Pattern for valid argument names.
   */
  val ArgPattern = "[a-z][a-zA-Z0-9_]+"

  /**
   * Regex for valid argument names.
   */
  val ArgRegex: Regex = ("^" + ArgPattern + "$").r

  /**
   * Return the default user, inferred from the environment of the shell user login. It is of course only
   * valid in a client context.
   */
  val DefaultUser: User = sys.env.get("ACCIO_USER").map(parseUser).getOrElse(User(sys.props("user.name")))

  /**
   * Generate a human-readable label for a list of parameters.
   *
   * @param params List of parameters.
   */
  def label(params: Seq[(String, Value)]): String = {
    params.map { case (k, v) =>
      var vStr = Values.toString(v)
      if (vStr.contains('/')) {
        // Remove any slash that would be polluting directory name.
        vStr = vStr.substring(vStr.lastIndexOf('/') + 1)
      }
      s"$k=$vStr"
    }.mkString(" ")
  }

  /**
   * Generate a human-readable label for a map of parameters.
   *
   * @param params Map of parameters.
   */
  def label(params: Map[String, Value]): String = label(params.toSeq)

  private[this] val UserRegex = "(.+)<(.+)>".r

  /**
   * Parse a string into a user. If it includes an email address, it should have the following
   * format: `User name <handle@domain.tld>`.
   *
   * @param str String to parse
   */
  def parseUser(str: String): User = str match {
    case UserRegex(name, email) => User(name.trim, Some(email.trim))
    case _ => User(str, None)
  }

  def toString(ref: Reference): String = s"${ref.node}/${ref.port}"

  def toString(user: User): String = s"${user.name}${user.email.map(email => s" <$email>").getOrElse("")}"

  def isCompleted(status: NodeStatus): Boolean = status match {
    case NodeStatus.Success => true
    case NodeStatus.Failed => true
    case NodeStatus.Killed => true
    case NodeStatus.Cancelled => true
    case NodeStatus.Lost => true
    case _ => false
  }

  def isCompleted(status: RunStatus): Boolean = status match {
    case RunStatus.Success => true
    case RunStatus.Failed => true
    case RunStatus.Killed => true
    case _ => false
  }
}