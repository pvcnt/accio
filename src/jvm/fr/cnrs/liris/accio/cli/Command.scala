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

package fr.cnrs.liris.accio.cli

import fr.cnrs.liris.common.flags.FlagsProvider

/**
 * A command that is part of a command-line application. Applications are divided into commands which can be invoked
 * independently. Commands contain the executable part of any application. All commands should be annotated with the
 * [[Cmd]] annotation.
 */
trait Command {
  /**
   * Execute this command.
   *
   * @param flags Input flags.
   * @param out   Report where to write progress.
   * @return Exit code.
   */
  def execute(flags: FlagsProvider, out: Reporter): ExitCode
}