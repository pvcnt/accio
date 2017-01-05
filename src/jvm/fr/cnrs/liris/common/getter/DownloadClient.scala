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

package fr.cnrs.liris.common.getter

import java.nio.file.{Path, Paths}

class DownloadClient(detector: Detector, getters: Map[String, Getter], decompressors: Map[String, Decompressor], detectPwd: Boolean) {
  def download(src: String, dst: Path): Unit = {
    val pwd = if (detectPwd) sys.props.get("user.dir").map(Paths.get(_)) else None
    detector.detect(src, pwd) match {
      case None => throw new DetectorException(s"Invalid source string: $src")
      case Some(detectedUri) =>
        // If there is a subdir component, then we download the root separately
        // and then copy over the proper subdir.
        /*val tmpDir = if (detectedUri.subdir.isDefined) {
          Some(Files.createTempDirectory("getter-"))
        } else None*/

        val scheme = detectedUri.getter.getOrElse(detectedUri.rawUri.getScheme)
        getters.get(scheme) match {
          case None => throw new IllegalArgumentException(s"Download not supported for scheme '$scheme'")
          case Some(getter) => getter.get(detectedUri.rawUri, dst)
        }
    }
  }
}