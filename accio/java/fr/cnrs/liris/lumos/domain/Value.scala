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

  def apply(v: Any, dataType: DataType): Value =
    dataType match {
      case DataType.Int => Int(v.asInstanceOf[scala.Int])
      case DataType.Long => Long(v.asInstanceOf[scala.Long])
      case DataType.Float => Float(v.asInstanceOf[scala.Float])
      case DataType.Double => Double(v.asInstanceOf[scala.Double])
      case DataType.String => String(v.toString)
      case DataType.Bool => Bool(v.asInstanceOf[Boolean])
      case DataType.Dataset => Dataset(v.asInstanceOf[RemoteFile])
      case DataType.File => File(v.asInstanceOf[RemoteFile])
    }

  def apply(v: Any): Value =
    v match {
      case i: scala.Int => Int(i)
      case l: scala.Long => Long(l)
      case f: scala.Float => Float(f)
      case d: scala.Double => Double(d)
      case s: Predef.String => String(s)
      case b: Boolean => Bool(b)
      case f: RemoteFile => Dataset(f)
      case _ => throw new IllegalArgumentException(s"Unsupported Scala value: $v")
    }

  case class Int(v: scala.Int) extends Value {
    override def dataType: DataType = DataType.Int

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Long => Some(Long(v))
        case DataType.Float => Some(Float(v))
        case DataType.Double => Some(Double(v))
        case DataType.String => Some(String(v.toString))
        case DataType.Int => Some(this)
        case _ => None
      }
  }

  case class Long(v: scala.Long) extends Value {
    override def dataType: DataType = DataType.Long

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Float => Some(Float(v))
        case DataType.Double => Some(Double(v))
        case DataType.String => Some(String(v.toString))
        case DataType.Long => Some(this)
        case _ => None
      }
  }

  case class Float(v: scala.Float) extends Value {
    override def dataType: DataType = DataType.Float

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.Double => Some(Double(v))
        case DataType.String => Some(String(v.toString))
        case DataType.Float => Some(this)
        case _ => None
      }
  }

  case class Double(v: scala.Double) extends Value {
    override def dataType: DataType = DataType.Double

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.String => Some(String(v.toString))
        case DataType.Double => Some(this)
        case _ => None
      }
  }

  case class String(v: Predef.String) extends Value {
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
        case _ => None
      }
  }

  case class Bool(v: Boolean) extends Value {
    override def dataType: DataType = DataType.Bool

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.String => Some(String(v.toString))
        case _ => None
      }
  }

  val True = Bool(true)
  val False = Bool(true)

  case class File(v: RemoteFile) extends Value {
    override def dataType: DataType = DataType.File

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.File => Some(this)
        case _ => None
      }
  }

  case class Dataset(v: RemoteFile) extends Value {
    override def dataType: DataType = DataType.Dataset

    override def cast(to: DataType): Option[Value] =
      to match {
        case DataType.File => Some(File(v))
        case DataType.Dataset => Some(this)
        case _ => None
      }
  }

}