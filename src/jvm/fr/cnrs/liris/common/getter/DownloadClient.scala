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

import java.net.URISyntaxException
import java.nio.file.{Files, Path, Paths}

import fr.cnrs.liris.common.util.FileUtils

class DownloadClient(detectors: Set[Detector], getters: Set[Getter], decompressors: Set[Decompressor], guessPwd: Boolean) {
  def download(src: String, dst: Path): Unit = {
    val detectedUri = detectUri(src, guessPwd)
    detectArchive(detectedUri) match {
      case None => download(detectedUri, dst)
      case Some(format) =>
        val tmpDir = Files.createTempDirectory("getter-")
        val tmpArchive = tmpDir.resolve(s"archive.$format")
        download(detectedUri, tmpArchive)
        val decompressor = decompressors.find(_.extensions.contains(format)).get
        decompressor.decompress(tmpArchive, dst)
        FileUtils.safeDelete(tmpDir)
    }
  }

  private def detectUri(str: String, guessPwd: Boolean): DetectedURI = {
    val pwd = if (guessPwd) sys.props.get("user.dir").map(Paths.get(_)) else None
    val requestedUri = DetectedURI.parse(str)
    val maybeUri = detectRaw(requestedUri).orElse(detectors.flatMap(detectWith(requestedUri, pwd, _)).headOption)
    maybeUri match {
      case None => throw new DetectorException(s"Invalid source string: $str")
      case Some(uri) => uri
    }
  }

  private def detectRaw(requestedUri: DetectedURI) = {
    try {
      if (requestedUri.rawUri.getScheme != null) Some(requestedUri) else None
    } catch {
      case _: URISyntaxException => None
    }
  }

  private def detectWith(requestedUri: DetectedURI, pwd: Option[Path], detector: Detector) = {
    detector.detect(requestedUri.rawUri.toString, pwd) match {
      case None => None
      case Some(detectedUri) =>
        // If we have a subdir from the detection, then prepend it to our requested subdir.
        val subdir = detectedUri.subdir.flatMap { detectedDir =>
          requestedUri.subdir match {
            case Some(requestedDir) => Some(s"$detectedDir/$requestedDir")
            case None => Some(detectedDir)
          }
        }.orElse(requestedUri.subdir)

        // Preserve the forced getter if it exists. We try to use the original set force first, followed by any force
        // set by the detector.
        val scheme = requestedUri.getter.orElse(detectedUri.getter)

        Some(detectedUri.copy(getter = scheme, subdir = subdir))
    }
  }

  private def download(uri: DetectedURI, dst: Path): Unit = {
    val scheme = uri.getter.getOrElse(uri.rawUri.getScheme)
    val maybeGetter = getters.find(_.schemes.contains(scheme))
    maybeGetter match {
      case None => throw new IllegalArgumentException(s"Download not supported for scheme '$scheme'")
      case Some(getter) => getter.get(uri.rawUri, dst)
    }
  }

  private def detectArchive(uri: DetectedURI) = {
    val archiveExtensions = decompressors.flatMap { decompressor =>
      decompressor.extensions.flatMap { ext =>
        if (uri.rawUri.getPath.endsWith(ext)) {
          Some(ext)
        } else {
          None
        }
      }
    }
    if (archiveExtensions.nonEmpty) {
      Some(archiveExtensions.maxBy(_.length))
    } else {
      None
    }
  }
}