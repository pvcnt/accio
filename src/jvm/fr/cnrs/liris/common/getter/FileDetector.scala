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

import java.net.URI
import java.nio.file.{Files, Path}

import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.common.util.OS

class FileDetector extends Detector with LazyLogging {
  override def detect(str: String, pwd: Option[Path]): Option[DetectedURI] = {
    if (str.isEmpty) {
      None
    } else if (str.charAt(0) == '/') {
      Some(formatUri(str))
    } else {
      pwd match {
        case None => throw new DetectorException("Relative paths require a pwd")
        case Some(path) => Some(absolutize(str, path))
      }
    }
  }

  private def formatUri(path: String) = {
    if (OS.Current == OS.Windows) {
      // Make sure we're using "/" on Windows. URLs are "/"-based.
      DetectedURI(new URI(s"file://${path.replace('\\', '/')}"))
    } else if (path.charAt(0) == '/') {
      DetectedURI(new URI(s"file://$path"))
    } else {
      DetectedURI(new URI(s"file:///$path"))
    }
  }

  private def absolutize(str: String, pwd: Path) = {
    val file = pwd.toFile
    if (!file.exists()) {
      logger.warn("Working directory does not exist")
    }
    val absolutePath = if (Files.isSymbolicLink(pwd)) {
      Files.readSymbolicLink(pwd).toAbsolutePath.resolve(str)
    } else {
      pwd.toAbsolutePath.resolve(str)
    }
    formatUri(absolutePath.normalize.toString)
  }
}