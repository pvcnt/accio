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

package fr.cnrs.liris.accio.core.uploader

import java.nio.file.Path

/**
 * Uploaders move files or directories to a remote location.
 *
 * The counterpart of an uploader is a downloader. The two inferfaces are split because while executors are usually
 * tied to a specific uploader, a downloader may support multiple ways of fetching data, thus guaranteeing
 * compatibility with multiple uploaders.
 */
trait Uploader {
  /**
   * Upload a path to a remote location. Source is guaranteed to exist, it can be either a file or a directory (both
   * must be supported). The key identifies where the source will be placed on the remote side, which is guaranteed
   * not to have been used previously with the same uploader. This method returns a URI, that should be designed to
   * be directly usable with a downloader.
   *
   * @param src Local source file or directory.
   * @param key Key identifying data on the remote side.
   * @return URI allowing to download data.
   */
  def upload(src: Path, key: String): String

  /**
   * Close this uploader. It can be used, for example, to terminate remote connections.
   */
  def close(): Unit = {}
}