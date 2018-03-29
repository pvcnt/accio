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

package fr.cnrs.liris.accio.tools.cli.commands

import java.util.NoSuchElementException

import com.google.inject.{Inject, Singleton}

/**
 * Store all commands known to the Accio CLI application. It is immutable, all commands should be
 * registered when the object is created.
 *
 * @param commands All known commands.
 */
final class CommandRegistry(val commands: Set[Command]) {
  /**
   * Return the command with a given name, if it exists.
   *
   * @param name Command name.
   */
  def get(name: String): Option[Command] = commands.find(_.name == name)

  /**
   * Return the command with a given name.
   *
   * @param name Command name.
   * @throws NoSuchElementException If no such command was found.
   */
  @throws[NoSuchElementException]
  def apply(name: String): Command = get(name).get
}

object CommandRegistry {
  def default: CommandRegistry =
    new CommandRegistry(Set(
      new ExportCommand,
      new GetCommand,
      new HelpCommand,
      new DescribeCommand,
      new KillCommand,
      new LogsCommand,
      new PushCommand,
      new DeleteCommand,
      new SubmitCommand,
      new ValidateCommand,
      new VersionCommand))
}
