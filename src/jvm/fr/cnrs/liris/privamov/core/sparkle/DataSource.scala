/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.privamov.core.sparkle

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import com.google.common.base.Charsets

trait DataSource[T] {
  def keys: Seq[String]

  def read(key: String): Iterable[T]
}

/**
 * A decoder converts a binary (record) into a plain object.
 *
 * @tparam T Plain object type
 */
trait Decoder[T] {
  /**
   * Decodes a binary record into an object.
   *
   * @param key   Key associated with the file containing this record
   * @param bytes Binary content
   * @return Plain object
   */
  def decode(key: String, bytes: Array[Byte]): Option[T]
}

class DirectorySource[T](url: String, extension: String, decoder: Decoder[T]) extends DataSource[T] {
  private[this] val path = Paths.get(url)

  override final def keys: Seq[String] =
    path.toFile.listFiles.map(_.toPath.getFileName.toString.dropRight(extension.length))

  override final def read(key: String): Iterable[T] =
    decoder.decode(key, Files.readAllBytes(path.resolve(s"$key$extension"))).toIterable
}

class TextLineDecoder[T](decoder: Decoder[T], headerLines: Int = 0, charset: Charset = Charsets.UTF_8) extends Decoder[Seq[T]] {
  override def decode(key: String, bytes: Array[Byte]): Option[Seq[T]] = {
    val lines = new String(bytes, charset).split("\n").drop(headerLines)
    val elements = lines.flatMap(line => decoder.decode(key, line.getBytes(charset)))
    if (elements.nonEmpty) Some(elements) else None
  }
}