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

import java.io.{PrintWriter, StringWriter}

import fr.cnrs.liris.util.StringUtils.maybe

case class ErrorDatum(mnemonic: String, message: Option[String], stacktrace: Seq[String])

object ErrorDatum {
  /**
   * Create a new error datum from a Java exception.
   *
   * @param e Java throwable.
   */
  def create(e: Throwable): ErrorDatum = {
    val writer = new StringWriter
    e.printStackTrace(new PrintWriter(writer, true))
    val stacktrace = writer.toString.split('\n')
    ErrorDatum(e.getClass.getName, Option(e.getMessage).flatMap(maybe), stacktrace)
  }
}
