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

sealed trait Value {
  def v: Any

  def dataType: Value.DataType
}

object Value {

  sealed trait DataType

  case object Integer extends DataType

  case object Number extends DataType

  case object String extends DataType

  case object Boolean extends DataType

  case object Dataset extends DataType

  case object Blob extends DataType

  case class Integer(v: Long) extends Value {
    override def dataType: DataType = Integer
  }

  case class Number(v: Double) extends Value {
    override def dataType: DataType = Number
  }

  case class String(v: Predef.String) extends Value {
    override def dataType: DataType = String
  }

  case class Boolean(v: scala.Boolean) extends Value {
    override def dataType: DataType = Boolean
  }

  case class Blob(v: RemoteFile) extends Value {
    override def dataType: DataType = Blob
  }

  case class Dataset(v: RemoteFile) extends Value {
    override def dataType: DataType = Dataset
  }

}