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

sealed trait DataType {
  def name: String

  override def toString: String = name
}

object DataType {

  case object Int extends DataType {
    override def name = "Int"
  }

  case object Long extends DataType {
    override def name = "Long"
  }

  case object Float extends DataType {
    override def name = "Float"
  }

  case object Double extends DataType {
    override def name = "Double"
  }

  case object String extends DataType {
    override def name = "String"
  }

  case object Bool extends DataType {
    override def name = "Bool"
  }

  case object Dataset extends DataType {
    override def name = "Dataset"
  }

  case object File extends DataType {
    override def name = "File"
  }

  def values: Seq[DataType] = Seq(Int, Long, Float, Double, String, Bool, Dataset, File)

  def valueOf(str: String): Option[DataType] = values.find(_.name == str)
}