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

package fr.cnrs.liris.infra.cli.app

/**
 * A command line exit code.
 *
 * @param code Numerical code.
 * @param name Machine name.
 */
case class ExitCode(code: Int, name: String)

object ExitCode {
  val Success = ExitCode(0, "SUCCESS")
  val CommandLineError = ExitCode(1, "COMMAND_LINE_ERROR")
  val DefinitionError = ExitCode(2, "DEFINITION_ERROR")
  val InternalError = ExitCode(3, "INTERNAL_ERROR")

  def select(codes: Seq[ExitCode]): ExitCode = {
    if (codes.contains(CommandLineError)) {
      CommandLineError
    } else if (codes.contains(DefinitionError)) {
      DefinitionError
    } else if (codes.contains(InternalError)) {
      InternalError
    } else {
      Success
    }
  }
}