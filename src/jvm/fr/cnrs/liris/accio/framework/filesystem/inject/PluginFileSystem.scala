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

package fr.cnrs.liris.accio.framework.filesystem.inject

import java.nio.file.Path

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.framework.filesystem.FileSystem

import scala.collection.immutable

@Singleton
final class PluginFileSystem @Inject()(plugins: immutable.Map[String, FileSystem]) extends FileSystem {
  require(plugins.nonEmpty, "No filesystem is provisioned")

  override def write(src: Path, uri: String): String = {
    val (prefix, filename) = split(uri)
    plugins.get(prefix) match {
      case None => throw new IllegalArgumentException(s"Unknown filesystem: $prefix (available are ${plugins.keySet.mkString(", ")})")
      case Some(fs) => fs.write(src, filename)
    }
  }

  override def read(uri: String, dst: Path): Unit = {
    val (prefix, filename) = split(uri)
    plugins.get(prefix) match {
      case None => throw new IllegalArgumentException(s"Unknown filesystem: $prefix (available are ${plugins.keySet.mkString(", ")})")
      case Some(fs) => fs.read(filename, dst)
    }
  }

  override def delete(uri: String): Unit = {
    val (prefix, filename) = split(uri)
    plugins.get(prefix) match {
      case None => throw new IllegalArgumentException(s"Unknown filesystem: $prefix (available are ${plugins.keySet.mkString(", ")})")
      case Some(fs) => fs.delete(filename)
    }
  }

  override def close(): Unit = plugins.values.foreach(_.close())

  private def split(uri: String): (String, String) = {
    uri.split("::") match {
      case Array(prefix, name) => (prefix, name)
      case Array(name) => ("posix", name)
      case _ => throw new IllegalArgumentException(s"Invalid filename: $uri")
    }
  }
}
