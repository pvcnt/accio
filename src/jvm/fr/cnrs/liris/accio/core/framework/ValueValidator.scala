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

package fr.cnrs.liris.accio.core.framework

import fr.cnrs.liris.accio.core.api.thrift.{ArgConstraint, ArgDef, InvalidSpecMessage}
import fr.cnrs.liris.dal.core.api._

import scala.collection.mutable

final class ValueValidator {
  def validate(value: Value, argDef: ArgDef, path: Option[String] = None): Seq[InvalidSpecMessage] = {
    if (value.kind != argDef.kind) {
      Seq(InvalidSpecMessage(s"Value of type ${DataTypes.toString(value.kind)} does not match argument of type ${DataTypes.toString(argDef.kind)}"))
    } else {
      val errors = mutable.ListBuffer.empty[InvalidSpecMessage]
      argDef.constraint.foreach { constraint =>
        checkConstraint(value, constraint, path, errors)
      }
      errors.toList
    }
  }

  private def checkConstraint(value: Value, constraint: ArgConstraint, path: Option[String], errors: mutable.ListBuffer[InvalidSpecMessage]) =
    value.kind match {
      case DataType(AtomicType.Byte, _) =>
        checkNumericConstraint(Values.decodeByte(value), constraint, path, errors)
      case DataType(AtomicType.Integer, _) =>
        checkNumericConstraint(Values.decodeInteger(value), constraint, path, errors)
      case DataType(AtomicType.Long, _) =>
        checkNumericConstraint(Values.decodeLong(value), constraint, path, errors)
      case DataType(AtomicType.Double, _) =>
        checkNumericConstraint(Values.decodeDouble(value), constraint, path, errors)
      case DataType(AtomicType.String, _) =>
        checkStringConstraint(Set(Values.decodeString(value)), constraint, path, errors)
      case DataType(AtomicType.Set, Seq(AtomicType.String)) =>
        val values = Values.decodeSet(value).asInstanceOf[Set[String]]
        checkStringConstraint(values, constraint, path, errors)
        checkListConstraint(values, constraint, path, errors)
      case DataType(AtomicType.List, Seq(AtomicType.String)) =>
        val values = Values.decodeList(value).asInstanceOf[Seq[String]]
        checkStringConstraint(values.toSet, constraint, path, errors)
        checkListConstraint(values, constraint, path, errors)
      case DataType(AtomicType.Set, _) => checkListConstraint(Values.decodeSet(value), constraint, path, errors)
      case DataType(AtomicType.List, _) => checkListConstraint(Values.decodeList(value), constraint, path, errors)
      case DataType(AtomicType.Map, _) => checkListConstraint(Values.decodeMap(value), constraint, path, errors)
      case _ => Seq.empty
    }

  private def checkNumericConstraint(value: Double, constraint: ArgConstraint, path: Option[String], errors: mutable.ListBuffer[InvalidSpecMessage]) = {
    constraint.minValue.foreach { minValue =>
      if (constraint.minInclusive.get && value < minValue) {
        errors += InvalidSpecMessage(s"Value must be >= $minValue", path)
      } else if (!constraint.minInclusive.get && value <= minValue) {
        errors += InvalidSpecMessage(s"Value must be > $minValue", path)
      }
    }
    constraint.maxValue.foreach { maxValue =>
      if (constraint.maxInclusive.get && value > maxValue) {
        errors += InvalidSpecMessage(s"Value must be <= $maxValue", path)
      } else if (!constraint.maxInclusive.get && value >= maxValue) {
        errors += InvalidSpecMessage(s"Value must be < $maxValue", path)
      }
    }
  }

  private def checkListConstraint(value: Iterable[_], constraint: ArgConstraint, path: Option[String], errors: mutable.ListBuffer[InvalidSpecMessage]) =
    checkNumericConstraint(value.size, constraint, path, errors)

  private def checkStringConstraint(values: Set[String], constraint: ArgConstraint, path: Option[String], errors: mutable.ListBuffer[InvalidSpecMessage]) = {
    if (constraint.allowedValues.nonEmpty) {
      if (values.intersect(constraint.allowedValues).size < values.size) {
        errors += InvalidSpecMessage(s"Value must be one of {${constraint.allowedValues.mkString(",")}}")
      }
    }
  }
}
