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

import fr.cnrs.liris.accio.api.thrift._

/**
 * Utils to deal with [[DataType]]s.
 */
object DataTypes {
  def isNumeric(dataType: DataType): Boolean =
    dataType match {
      case DataType.Atomic(tpe) => isNumeric(tpe)
      case DataType.ListType(ListType(values)) => isNumeric(values)
      case DataType.MapType(MapType(_, values)) => isNumeric(values)
      case _ => false
    }

  private def isNumeric(dataType: AtomicType): Boolean =
    dataType match {
      case AtomicType.Integer => true
      case AtomicType.Long => true
      case AtomicType.Float => true
      case AtomicType.Double => true
      case AtomicType.Distance => true
      case AtomicType.Duration => true
      case _ => false
    }

  def stringify(dataType: DataType): String =
    dataType match {
      case DataType.Atomic(tpe) => stringify(tpe)
      case DataType.ListType(ListType(values)) => s"list(${stringify(values)})"
      case DataType.MapType(MapType(keys, values)) =>
        s"map(${stringify(keys)}, ${stringify(values)})"
      case DataType.Dataset(DatasetType(schema)) =>
        s"dataset(${schema.map { case (k, v) => s"$k=${stringify(v)}" }.mkString(", ")})"
      case DataType.UnknownUnionField(_) => "???"
    }

  def stringify(atomicType: AtomicType): String = {
    atomicType.getClass.getSimpleName.stripSuffix("$").toLowerCase
  }
}