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

import java.net.URISyntaxException
import java.nio.file.Path

class ComposedDetector(detectors: Seq[Detector]) extends Detector {
  override def detect(str: String, pwd: Option[Path]): Option[DetectedURI] = {
    val requestedUri = DetectedURI.parse(str)
    detectRaw(requestedUri)
      .orElse(detectors.flatMap(detectWith(requestedUri, pwd, _)).headOption)
  }

  private def detectRaw(requestedUri: DetectedURI) = {
    try {
      if (requestedUri.rawUri.getScheme.nonEmpty) Some(requestedUri) else None
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
}