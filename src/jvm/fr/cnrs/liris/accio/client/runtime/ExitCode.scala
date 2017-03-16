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

package fr.cnrs.liris.accio.client.runtime

/**
 * A command line exit code.
 *
 * @param code Numerical code.
 * @param name Machine name.
 */
case class ExitCode(code: Int, name: String)

/**
 * Factory for [[ExitCode]].
 */
object ExitCode {
  val Success = ExitCode(0, "SUCCESS")
  val CommandLineError = ExitCode(1, "COMMAND_LINE_ERROR")
  val DefinitionError = ExitCode(2, "DEFINITION_ERROR")
  val RuntimeError = ExitCode(4, "RUNTIME_ERROR")
  val InternalError = ExitCode(5, "INTERNAL_ERROR")

  def values: Seq[ExitCode] = Seq(CommandLineError, DefinitionError, RuntimeError, InternalError, Success)

  def select(codes: Seq[ExitCode]): ExitCode = {
    values.foreach { code =>
      if (codes.contains(code)) {
        return code
      }
    }
    ExitCode.Success
  }
}