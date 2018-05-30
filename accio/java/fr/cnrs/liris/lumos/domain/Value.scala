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

package fr.cnrs.liris.lumos.domain

import com.twitter.util.Try

sealed trait Value {
  def v: Any

  def cast(to: DataType): Option[Value]

  def dataType: DataType
}

object Value {

  def apply(v: Any, dataType: DataType): Value = {
    dataType match {
      case DataType.Int => Int(v.asInstanceOf[DataType.Int.JvmType])
      case DataType.Long => Long(v.asInstanceOf[DataType.Long.JvmType])
      case DataType.Float => Float(v.asInstanceOf[DataType.Float.JvmType])
      case DataType.Double => Double(v.asInstanceOf[DataType.Double.JvmType])
      case DataType.String => String(v.toString)
      case DataType.Bool => Bool(v.asInstanceOf[DataType.Bool.JvmType])
      case DataType.Dataset => Dataset(v.asInstanceOf[DataType.Dataset.JvmType])
      case DataType.File => File(v.asInstanceOf[DataType.File.JvmType])
      case d: DataType.UserDefined => UserDefined(v.asInstanceOf[d.JvmType], d)
    }
  }

  def apply(v: Any): Value = {
    DataType.values.find(_.cls.runtimeClass.isAssignableFrom(v.getClass)) match {
      case Some(d) => apply(v, d)
      case None => throw new IllegalArgumentException(s"Unsupported Scala value: $v")
    }
  }

  case class Int(v: DataType.Int.JvmType) extends Value {
    override def dataType: DataType = DataType.Int

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Long => Some(Long(v))
        case DataType.Float => Some(Float(v))
        case DataType.Double => Some(Double(v))
        case DataType.String => Some(String(v.toString))
        case DataType.Int => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class Long(v: DataType.Long.JvmType) extends Value {
    override def dataType: DataType = DataType.Long

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.String => Some(String(v.toString))
        //case DataType.Double => Some(Double(v))
        //case DataType.Float if v <= scala.Float.MaxValue => Some(Float(v))
        //case DataType.Int if v <= scala.Int.MaxValue => Some(Int(v.toInt))
        case DataType.Long => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class Float(v: DataType.Float.JvmType) extends Value {
    override def dataType: DataType = DataType.Float

    override def cast(to: DataType): Option[Value] =
      to match {
        //case DataType.Int if DoubleMath.isMathematicalInteger(v) => Some(Int(v.toInt))
        //case DataType.Long if DoubleMath.isMathematicalInteger(v) => Some(Long(v.toLong))
        case DataType.Double => Some(Double(v))
        case DataType.String => Some(String(v.toString))
        case DataType.Float => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class Double(v: DataType.Double.JvmType) extends Value {
    override def dataType: DataType = DataType.Double

    override def cast(to: DataType): Option[Value] =
      to match {
        //case DataType.Float if v <= scala.Float.MaxValue => Some(Float(v.toFloat))
        //case DataType.Int if DoubleMath.isMathematicalInteger(v) => Some(Int(v.toInt))
        //case DataType.Long if DoubleMath.isMathematicalInteger(v) => Some(Long(v.toLong))
        case DataType.String => Some(String(v.toString))
        case DataType.Double => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class String(v: DataType.String.JvmType) extends Value {
    override def dataType: DataType = DataType.String

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Int => Try(v.toInt).toOption.map(Value.Int)
        case DataType.Long => Try(v.toLong).toOption.map(Value.Long)
        case DataType.Float => Try(v.toFloat).toOption.map(Value.Float)
        case DataType.Double => Try(v.toDouble).toOption.map(Value.Double)
        case DataType.Bool =>
          v match {
            case "true" | "t" | "yes" | "y" | "1" => Some(Value.True)
            case "false" | "f" | "no" | "n" | "0" => Some(Value.False)
            case _ => None
          }
        case DataType.String => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class Bool(v: DataType.Bool.JvmType) extends Value {
    override def dataType: DataType = DataType.Bool

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.String => Some(String(v.toString))
        case DataType.Bool => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  val True = Bool(true)

  val False = Bool(false)

  case class File(v: DataType.File.JvmType) extends Value {
    override def dataType: DataType = DataType.File

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Dataset => Some(Value.Dataset(v))
        case DataType.File => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class Dataset(v: DataType.Dataset.JvmType) extends Value {
    override def dataType: DataType = DataType.Dataset

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.File => Some(File(v))
        case DataType.Dataset => Some(this)
        case d: DataType.UserDefined => d.decode(this).map(v => UserDefined(v, d))
        case _ => None
      }
  }

  case class UserDefined(v: Any, dataType: DataType.UserDefined) extends Value {
    override def cast(to: DataType): Option[Value] = {
      dataType.encode(v.asInstanceOf[dataType.JvmType]).cast(to)
    }
  }

  case class Unresolved(value: Value, originalDataType: Predef.String) extends Value {
    override def v: Any = value.v

    override def dataType: DataType = value.dataType

    override def cast(to: DataType): Option[Value] = value.cast(to)
  }

}