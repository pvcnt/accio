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

package fr.cnrs.liris.common.getter

import java.io.IOException
import java.net.URI
import java.nio.file.Path

/**
 * Getter defines the interface that schemes must implement to download this.
 */
trait Getter {
  /**
   * GetFile downloads the give URL into the given path. The URL must reference a single file. If possible, the
   * Getter should check if the remote end contains the same file and no-op this operation.
   *
   * Destination path is not guaranteed to exist.
   *
   * @param src Source URI to download.
   * @param dst Destination path where to write the file.
   * @throws IOException If something wrong occurred while download the file.
   */
  @throws[IOException]
  def get(src: URI, dst: Path): Unit

  /**
   * Return the list of schemes supported by this getter.
   */
  def schemes: Set[String]
}