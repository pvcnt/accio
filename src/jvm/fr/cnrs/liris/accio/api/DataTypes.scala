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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift.{AtomicType, DataType}

/**
 * Utils to deal with [[DataType]]s.
 */
object DataTypes {
  // In the following regex, +? is a reluctant quantifier (i.e., not greedy).
  private[this] val ListRegex = "^list\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val SetRegex = "^set\\s*\\(\\s*(.+?)\\s*\\)$".r
  private[this] val MapRegex = "^map\\s*\\(\\s*(.+?),\\s*(.+?)\\s*\\)$".r

  def parse(str: String): DataType =
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
      case AtomicType.List => isNumeric(dataType.args.head)
      case AtomicType.Set => isNumeric(dataType.args.head)
      case AtomicType.Map => isNumeric(dataType.args.last)
      case _ => isNumeric(dataType.base)
    }

  private def isNumeric(dataType: AtomicType): Boolean =
    dataType match {
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
      case AtomicType.Map => s"map(${toString(dataType.args.head)}, ${toString(dataType.args.last)})"
      case base => toString(base)
    }

  def toString(atomicType: AtomicType): String = atomicType.getClass.getSimpleName.stripSuffix("$").toLowerCase
}