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

package fr.cnrs.liris.sparkle.format

/**
 * Exception thrown when a parser meets a bad record and can't parse it.
 *
 * @param record   The record the parser failed to parse
 * @param location The location of the record inside the stream, if possible to determine.
 * @param cause    The actual exception about why the record is bad and can't be parsed.
 */
final class BadRecordException(
  val record: String,
  val location: Option[String] = None,
  cause: Throwable = null)
  extends Exception(s"Bad record found ${location.map(" at " + _).getOrElse("")}: $record", cause)
