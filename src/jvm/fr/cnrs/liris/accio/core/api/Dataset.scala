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

package fr.cnrs.liris.accio.core.api

/**
 * Descriptor of a dataset that can be used as a data type for operators. It is basically just a handle to an URI
 * where the dataset is written.
 *
 * @param uri    URI where the dataset is written.
 * @param format Format under which the dataset is written.
 */
case class Dataset(uri: String, format: String)

/**
 * Factory for [[Dataset]].
 */
object Dataset {
  private[this] val DefaultFormat = "csv"

  /**
   * Parse a string into a dataset.
   *
   * @param str String to parse.
   */
  def parse(str: String): Dataset = str.split(":") match {
    case Array(format, uri) => new Dataset(uri, format)
    case Array(uri) => new Dataset(uri, DefaultFormat)
    case _ => throw new IllegalArgumentException(s"Invalid dataset: $str")
  }
}
