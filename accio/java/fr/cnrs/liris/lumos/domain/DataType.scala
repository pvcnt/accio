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

import com.google.common.annotations.VisibleForTesting

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

sealed trait DataType {
  type JvmType

  def name: String

  def cls: ClassTag[JvmType]

  override def toString: String = name
}

object DataType {
  // It is extremely important that in the following list File starts BEFORE Dataset. Indeed,
  // both have the same JvmType (RemoteFile), we prefer to consider by default files (as all
  // datasets are files, but not all files are datasets).
  private[this] val builtIn = Seq(Int, Long, Float, Double, String, Bool, File, Dataset)
  private[this] val custom = mutable.Set.empty[Custom]

  case object Int extends DataType {
    override type JvmType = scala.Int

    override def cls: ClassTag[JvmType] = classTag[scala.Int]

    override def name = "Int"
  }

  case object Long extends DataType {
    override type JvmType = scala.Long

    override def cls: ClassTag[JvmType] = classTag[scala.Long]

    override def name = "Long"
  }

  case object Float extends DataType {
    override type JvmType = scala.Float

    override def cls: ClassTag[JvmType] = classTag[scala.Float]

    override def name = "Float"
  }

  case object Double extends DataType {
    override type JvmType = scala.Double

    override def cls: ClassTag[JvmType] = classTag[scala.Double]

    override def name = "Double"
  }

  case object String extends DataType {
    override type JvmType = Predef.String

    override def cls: ClassTag[JvmType] = classTag[Predef.String]

    override def name = "String"
  }

  case object Bool extends DataType {
    override type JvmType = scala.Boolean

    override def cls: ClassTag[JvmType] = classTag[scala.Boolean]

    override def name = "Bool"
  }

  case object Dataset extends DataType {
    override type JvmType = RemoteFile

    override def cls: ClassTag[JvmType] = classTag[RemoteFile]

    override def name = "Dataset"
  }

  case object File extends DataType {
    override type JvmType = RemoteFile

    override def cls: ClassTag[JvmType] = classTag[RemoteFile]

    override def name = "File"
  }

  trait Custom extends DataType {
    def base: DataType

    def encode(v: JvmType): Value

    def decode(value: Value): Option[JvmType]
  }

  def register(dataType: Custom): Unit = custom += dataType

  @VisibleForTesting
  private[domain] def clear(): Unit = custom.clear()

  def values: Seq[DataType] = builtIn ++ custom.map(_.asInstanceOf[DataType])

  def find(cls: Class[_]): Option[DataType] = values.find(_.cls.runtimeClass.isAssignableFrom(cls))

  def find(cls: ClassTag[_]): Option[DataType] = find(cls.runtimeClass)

  def parse(str: String): Option[DataType] = values.find(_.name == str)
}