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

package fr.cnrs.liris.accio.core.service

import java.nio.file.Path

/**
 * Uploaders move files or directories to another location. They should be designed to work conjointly with the
 * common/getter module, as uploaders do not provide any method to retrieve uploaded data.
 */
trait Uploader {
  /**
   * Upload a path to another location. Source is guaranteed to exist, it can be either a file or a directory (both
   * must be supported). The key identifies where the source will be placed on the remote side, which is guaranteed
   * not to have been used previously with the same uploader. This method returns a URI, that should be designed to
   * be directly usable with the common/getter module.
   *
   * @param src Local source file or directory.
   * @param key Key identifying data on the remote side.
   * @return URI allowing to download data.
   */
  def upload(src: Path, key: String): String
}
