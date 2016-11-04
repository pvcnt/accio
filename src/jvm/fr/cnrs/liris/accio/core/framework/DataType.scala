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
 * Type for data operators can manipulate. All input/output arguments of operators must be mappable to one of
 * these types. It is only enforced at runtime, when operator definitions are parsed.
 */
sealed trait DataType {
  @JsonValue
  override def toString: String = getClass.getSimpleName.stripSuffix("$").toLowerCase

  /**
   * Return a human-readable description for what valus of this type represent.
   */
  def typeDescription: String

  /**
   * Specifies whether this data type behaves like a numeric type.
   */
  def isNumeric: Boolean = false
}

/**
 * List of available [[DataType]]s and factory for [[DataType]].
 */
object DataType {
  // In the three following regex, +? is a so-called reluctant quantifier (i.e., not greedy).
  private[this] val ListRegex = "^list\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val SetRegex = "^set\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val MapRegex = "^map\\s*\\(\\s*(.+?),\\s*(.+?)\\s*\\)$".r

  /**
   * A Java/Scala byte data type.
   */
  case object Byte extends DataType {
    override def typeDescription: String = "byte"

    override def isNumeric: Boolean = true
  }

  /**
   * A Java/Scala short data type.
   */
  case object Short extends DataType {
    override def typeDescription: String = "short"

    override def isNumeric: Boolean = true
  }

  /**
   * A Java/Scala integer data type.
   */
  case object Integer extends DataType {
    override def typeDescription: String = "integer"

    override def isNumeric: Boolean = true
  }

  /**
   * A Java/Scala long data type.
   */
  case object Long extends DataType {
    override def typeDescription: String = "long"

    override def isNumeric: Boolean = true
  }

  /**
   * A Java/Scala double data type.
   */
  case object Double extends DataType {
    override def typeDescription: String = "float"

    override def isNumeric: Boolean = true
  }

  /**
   * A Java/Scala string data type.
   */
  case object String extends DataType {
    override def typeDescription: String = "string"
  }

  /**
   * A Java/Scala boolean data type.
   */
  case object Boolean extends DataType {
    override def typeDescription: String = "boolean"
  }

  /**
   * A geographical location data type, mapped to a [[Location]].
   */
  case object Location extends DataType {
    override def typeDescription: String = "geo-location"
  }

  /**
   * A timestamp (without time zone) data type, mapped to a [[org.joda.time.Instant]].
   */
  case object Timestamp extends DataType {
    override def typeDescription: String = "timestamp"
  }

  /**
   * A duration data type, mapped to a [[org.joda.time.Duration]].
   */
  case object Duration extends DataType {
    override def typeDescription: String = "duration"

    override def isNumeric: Boolean = true
  }

  /**
   * A distance data type, mapped to a [[fr.cnrs.liris.common.geo.Distance]].
   */
  case object Distance extends DataType {
    override def typeDescription: String = "distance"

    override def isNumeric: Boolean = true
  }

  /**
   * A dataset data type, mapped to a [[fr.cnrs.liris.accio.core.api.Dataset]].
   */
  case object Dataset extends DataType {
    override def typeDescription: String = "dataset"
  }

  /**
   * A dataset data type, mapped to a [[fr.cnrs.liris.accio.core.api.Image]].
   */
  case object Image extends DataType {
    override def typeDescription: String = "image"
  }

  /**
   * A list data type, mapped to Scala [[Seq]], embedding of any valid other data type.
   */
  case class List(of: DataType) extends DataType {
    override def toString: String = s"list($of)"

    override def typeDescription: String = s"list of ${of.typeDescription}s"

    override def isNumeric: Boolean = of.isNumeric
  }

  /**
   * A Scala [[Set]] data type, embedding of any valid other data type.
   */
  case class Set(of: DataType) extends DataType {
    override def toString: String = s"set($of)"

    override def typeDescription: String = s"set of ${of.typeDescription}s"

    override def isNumeric: Boolean = of.isNumeric
  }

  /**
   * A Scala [[Map]] data type, embedding of any valid other data type.
   */
  case class Map(ofKeys: DataType, ofValues: DataType) extends DataType {
    override def toString: String = s"map($ofKeys,$ofValues)"

    override def typeDescription: String = {
      s"map of ${ofKeys.typeDescription}s => ${ofValues.typeDescription}s"
    }

    override def isNumeric: Boolean = ofValues.isNumeric
  }

  /**
   * Parse a string into a data type.
   *
   * @param str String to parse.
   * @throws IllegalArgumentException If the string does not represent a valid data type.
   */
  def parse(str: String): DataType = str.toLowerCase.trim match {
    case "byte" => Byte
    case "short" => Short
    case "integer" => Integer
    case "long" => Long
    case "double" => Double
    case "string" => String
    case "boolean" => Boolean
    case "location" => Location
    case "timestamp" => Timestamp
    case "duration" => Duration
    case "distance" => Distance
    case "dataset" => Dataset
    case "image" => Image
    case ListRegex(of) => List(parse(of))
    case SetRegex(of) => Set(parse(of))
    case MapRegex(ofKeys, ofValues) => Map(parse(ofKeys), parse(ofValues))
    case _ => throw new IllegalArgumentException(s"Invalid data type: $str")
  }

}