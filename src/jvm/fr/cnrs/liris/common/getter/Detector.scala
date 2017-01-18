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

import java.nio.file.Path

/**
 * Invalid URLs or a URLs with a blank scheme are passed through a detector in order to determine if it is
 * a shorthand for something else well-known.
 */
trait Detector {
  /**
   * Detect whether the string matches a known pattern and turn it into a proper URL.
   *
   * This method should only throw an exception if it is certain that the string corresponds to a pattern it handles
   * but for some reason it is incorrectly formatted. If the error is recoverable and the string can still be
   * detected by another detector, [[None]] should be returned without any exception.
   *
   * @param str String to test.
   * @param pwd Current working directory.
   * @throws DetectorException If a fatal error preventing detection occurred.
   * @return Detected URI, if any.
   */
  @throws[DetectorException]
  def detect(str: String, pwd: Option[Path] = None): Option[DetectedURI]
}

/**
 *
 * @param message
 * @param cause
 */
class DetectorException(message: String, cause: Throwable = null) extends Exception(message, cause)