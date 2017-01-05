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

import java.net.URI
import java.nio.file.Path

/**
 * Detector infers from a string the getter to use and the associated URI.
 */
trait Detector {
  /**
   * Detect whether the string matches a known pattern and turn it into a proper URL.
   *
   * @param str String to test.
   * @param pwd Current working directory.
   * @throws DetectorException If a fatal error occurred when parsing a string.
   * @return Detected URI, if any.
   */
  @throws[DetectorException]
  def detect(str: String, pwd: Option[Path] = None): Option[DetectedURI]
}

/**
 *
 * @param rawUri
 * @param getter
 * @param subdir
 */
case class DetectedURI(rawUri: URI, getter: Option[String] = None, subdir: Option[String] = None) {
  override def toString: String = {
    getter.map(_ + "::").getOrElse("") + rawUri + subdir.map("//" + _).getOrElse("")
  }
}

/**
 * Factory for [[DetectedURI]].
 */
object DetectedURI {
  // Regular expression that finds forced getters. This syntax is schema::url, example: git::https://foo.com
  private[this] val ForcedRegex = "^([A-Za-z0-9]+)::(.+)$".r

  // getForcedGetter takes a source and returns the tuple of the forced
  // getter and the raw URL (without the force syntax).
  // SourceDirSubdir takes a source and returns a tuple of the URL without
  // the subdir and the URL with the subdir.
  def parse(str: String): DetectedURI = {
    val (forcedGetter, rawStr) = {
      val matches = ForcedRegex.findAllMatchIn(str)
      if (matches.hasNext) {
        val m = matches.next
        (Some(m.group(1)), m.group(2))
      } else {
        (None, str)
      }
    }

    val (rawUri, subdir) = {
      // Calculate an offset to avoid accidentally marking the scheme as the dir.
      var idx = rawStr.indexOf("://")
      val offset = if (idx > -1) idx + 3 else 0

      // First see if we even have an explicit subdir
      idx = rawStr.drop(offset).indexOf("//")
      if (idx == -1) {
        (rawStr, None)
      } else {
        idx += offset
        var rawUri = rawStr.take(idx)
        var subdir = rawStr.drop(idx + 2)

        // Next, check if we have query parameters and push them onto the URL.
        idx = subdir.indexOf("?")
        if (idx > -1) {
          subdir = subdir.take(idx)
          rawUri += subdir.drop(idx)
        }
        (rawUri, Some(subdir))
      }
    }

    DetectedURI(new URI(rawUri), forcedGetter, subdir)
  }
}

/**
 *
 * @param message
 * @param cause
 */
class DetectorException(message: String, cause: Throwable = null) extends Exception(message, cause)