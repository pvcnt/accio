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

import java.util.Locale

import com.google.common.annotations.VisibleForTesting

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

/**
 * A data type is used to characterise the nature of the pieces of information that Lumos
 * manipulates. Indeed, every [[Value]] is bound to a specific data type that is used to validate
 * its correctness and allow to build rich user interfaces. Built-in data types are provided by
 * Lumos itself, but custom data types can be implemented by extending the [[DataType.UserDefined]]
 * trait and registering it with [[DataType.register]].
 *
 * A data type is bound to one (and only one) JVM type, even though nothing prevents the same JVM
 * type to be bound to several data types. However, this is not recommended for user-defined data
 * types.
 */
sealed trait DataType {
  /**
   * The JVM type that this data type is bound to. Even though Scala offers no way to force that,
   * this has to be redefined by implementations.
   */
  type JvmType

  /**
   * Return the name of this data type. It should be short and unique across all data types.
   */
  def name: String

  /**
   * Return a short human-readable description characterising values of this data type. For example,
   * a data type representing a timestamp could provide as help "a timestamp".
   */
  def help: String

  /**
   * Return the class tag for values of this data type.
   */
  def cls: ClassTag[JvmType]

  override def toString: String = name
}

object DataType {
  // It is important that in the following list File starts BEFORE Dataset. Indeed, both have
  // the same JvmType (RemoteFile), we prefer to consider that by default RemoteFile's are files
  // and not datasets (as all datasets are files, but not all files are datasets).
  private[this] val builtIn = Seq(Int, Long, Float, Double, String, Bool, File, Dataset)
  private[this] val custom = mutable.Set.empty[UserDefined]

  case object Int extends DataType {
    override type JvmType = scala.Int

    override def cls: ClassTag[JvmType] = classTag[scala.Int]

    override def name = "Int"

    override def help = "a 32-bit integer"
  }

  case object Long extends DataType {
    override type JvmType = scala.Long

    override def cls: ClassTag[JvmType] = classTag[scala.Long]

    override def name = "Long"

    override def help = "a 64-bit integer"
  }

  case object Float extends DataType {
    override type JvmType = scala.Float

    override def cls: ClassTag[JvmType] = classTag[scala.Float]

    override def name = "Float"

    override def help = "a 32-bit float"
  }

  case object Double extends DataType {
    override type JvmType = scala.Double

    override def cls: ClassTag[JvmType] = classTag[scala.Double]

    override def name = "Double"

    override def help = "a 64-bit float"
  }

  case object String extends DataType {
    override type JvmType = Predef.String

    override def cls: ClassTag[JvmType] = classTag[Predef.String]

    override def name = "String"

    override def help = "a string"
  }

  case object Bool extends DataType {
    override type JvmType = scala.Boolean

    override def cls: ClassTag[JvmType] = classTag[scala.Boolean]

    override def name = "Bool"

    override def help = "a boolean"
  }

  case object Dataset extends DataType {
    override type JvmType = RemoteFile

    override def cls: ClassTag[JvmType] = classTag[RemoteFile]

    override def name = "Dataset"

    override def help = "a dataset"
  }

  case object File extends DataType {
    override type JvmType = RemoteFile

    override def cls: ClassTag[JvmType] = classTag[RemoteFile]

    override def name = "File"

    override def help = "a file"
  }

  trait UserDefined extends DataType {
    def encode(v: JvmType): Value

    def decode(value: Value): Option[JvmType]
  }

  /**
   * Register a new user-defined data type. It has no effect if the data type was already
   * registered.
   *
   * @param dataType Data type to register
   */
  def register(dataType: UserDefined): Unit = {
    // TODO: throw exception if a data type with the same name already exists (but is not the same instance).
    custom += dataType
  }

  @VisibleForTesting
  private[domain] def clear(): Unit = custom.clear()

  def values: Seq[DataType] = builtIn ++ custom.map(_.asInstanceOf[DataType])

  def find(cls: Class[_]): Option[DataType] = values.find(_.cls.runtimeClass.isAssignableFrom(cls))

  def find(cls: ClassTag[_]): Option[DataType] = find(cls.runtimeClass)

  def parse(str: String): Option[DataType] = {
    val lowercaseStr = str.toLowerCase(Locale.ROOT)
    values.find(_.name.toLowerCase(Locale.ROOT) == lowercaseStr)
  }
}