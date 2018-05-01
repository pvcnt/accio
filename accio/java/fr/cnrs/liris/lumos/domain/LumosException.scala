/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.lumos.domain

sealed abstract class LumosException(message: String) extends Exception(message)

object LumosException {
  case class AlreadyExists(resourceType: String, resourceName: String)
    extends LumosException(s"The $resourceType resource already exists: $resourceName")

  case class NotFound(resourceType: String, resourceName: String)
    extends LumosException(s"The $resourceType resource was not found: $resourceName")

  case class InvalidArgument(errors: Seq[String])
    extends LumosException(s"Some arguments were invalid:\n  ${errors.mkString("\n  ")}")

  case class FailedPrecondition(errors: Seq[String])
    extends LumosException(s"Some preconditions failed:\n  ${errors.mkString("\n  ")}")
}