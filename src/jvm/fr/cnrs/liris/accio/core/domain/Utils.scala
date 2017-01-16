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

package fr.cnrs.liris.accio.core.domain

import scala.util.matching.Regex

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
   * Pattern for valid argument names.
   */
  val ArgPattern = "[a-z][a-zA-Z0-9_]+"

  /**
   * Regex for valid argument names.
   */
  val ArgRegex: Regex = ("^" + ArgPattern + "$").r

  // In the three following regex, +? is a so-called reluctant quantifier (i.e., not greedy).
  private[this] val ListRegex = "^list\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val SetRegex = "^set\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val MapRegex = "^map\\s*\\(\\s*(.+?),\\s*(.+?)\\s*\\)$".r
  private[this] val UserRegex = "(.+)<(.+)>".r

  /**
   * Return the default user, inferred from the shell user login. It has no email address. It is of course only
   * valid in a client context.
   */
  val DefaultUser = User(sys.props("user.name"))

  /**
   * Generate a human-readable label for a list of parameters.
   *
   * @param params List of parameters.
   */
  def label(params: Seq[(String, Value)]): String = {
    params.map { case (k, v) =>
      var vStr = v.toString
      if (vStr.contains('/')) {
        // Remove any slash that would be polluting directory name.
        vStr = vStr.substring(vStr.lastIndexOf('/') + 1)
      }
      s"$k=$vStr"
    }.mkString(",")
  }

  /**
   * Generate a human-readable label for a map of parameters.
   *
   * @param params Map of parameters.
   */
  def label(params: Map[String, Value]): String = label(params.toSeq)

  def parseDataType(str: String): DataType =
    str.toLowerCase.trim match {
      case ListRegex(of) => DataType(AtomicType.List, Seq(parseAtomicType(of)))
      case SetRegex(of) => DataType(AtomicType.Set, Seq(parseAtomicType(of)))
      case MapRegex(ofKeys, ofValues) => DataType(AtomicType.Map, Seq(parseAtomicType(ofKeys), parseAtomicType(ofValues)))
      case s => DataType(parseAtomicType(s))
    }

  private def parseAtomicType(str: String): AtomicType =
    str.toLowerCase.trim match {
      case "byte" => AtomicType.Byte
      case "integer" => AtomicType.Integer
      case "long" => AtomicType.Long
      case "double" => AtomicType.Double
      case "string" => AtomicType.String
      case "boolean" => AtomicType.Boolean
      case "location" => AtomicType.Location
      case "timestamp" => AtomicType.Timestamp
      case "duration" => AtomicType.Duration
      case "distance" => AtomicType.Distance
      case "dataset" => AtomicType.Dataset
      case _ => throw new IllegalArgumentException(s"Invalid data type: $str")
    }

  def isNumeric(dataType: DataType): Boolean =
    dataType.base match {
      case AtomicType.Byte => true
      case AtomicType.Integer => true
      case AtomicType.Long => true
      case AtomicType.Double => true
      case AtomicType.Distance => true
      case AtomicType.Duration => true
      case _ => false
    }

  def toString(dataType: DataType): String =
    dataType.base match {
      case AtomicType.List => s"list(${toString(dataType.args.head)})"
      case AtomicType.Set => s"set(${toString(dataType.args.head)})"
      case AtomicType.Map => s"map(${toString(dataType.args.head)},${toString(dataType.args.last)})"
      case base => toString(base)
    }

  private def toString(atomicType: AtomicType): String = atomicType.getClass.getSimpleName.stripSuffix("$").toLowerCase

  def describe(dataType: DataType): String =
    dataType.base match {
      case AtomicType.List => s"list of ${describe(dataType.args.head)}"
      case AtomicType.Set => s"set of ${describe(dataType.args.head)}"
      case AtomicType.Map => s"map of ${describe(dataType.args.head)} => ${describe(dataType.args.last)}"
      case base => toString(base)
    }

  def describe(atomicType: AtomicType): String = toString(atomicType)

  /**
   * Parse a string into a dependency.
   *
   * @param str String to parse.
   * @throws IllegalArgumentException If the string is not formatted as a valid reference.
   */
  @throws[IllegalArgumentException]
  def parseReference(str: String): Reference = str.split("/") match {
    case Array(node, port) =>
      require(node.nonEmpty, s"Invalid reference, empty node name: $str")
      require(port.nonEmpty, s"Invalid reference, empty port name: $str")
      Reference(node, port)
    case _ => throw new IllegalArgumentException(s"Invalid reference: $str")
  }

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
    case _ => false
  }

  def isCompleted(status: RunStatus): Boolean = status match {
    case RunStatus.Success => true
    case RunStatus.Failed => true
    case RunStatus.Killed => true
    case _ => false
  }
}