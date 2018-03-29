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

import com.twitter.app.Flags
import com.twitter.conversions.time._
import fr.cnrs.liris.accio.tools.cli.event.{Reporter, TerminalEventHandlerOptions}

/**
 * A command that is part of a command-line application.
 */
trait Command {
  private[commands] val flag = new Flags("accio", includeGlobal = false, failFastUntilParsed = true)
  // A nice middle ground; snappy but not too spammy in logs.
  private[commands] val showProgressRateFlag = flag("show_progress_rate", 30.milliseconds, "Minimum duration between progress messages in the output.")
  private[commands] val showProgressFlag = flag("show_progress", true, "Display progress messages.")
  private[commands] val showTaskFinishFlag = flag("show_task_finish", false, "Display progress messages when tasks complete, not just when they start.")
  private[commands] val colorFlag = flag("color", true, "Use terminal controls to colorize output.")
  private[commands] val cursesFlag = flag("curses", true, "Use terminal cursor controls to minimize scrolling output")
  private[commands] val quietFlag = flag("quiet", false, "Suppress all output, even for errors. Use exit code to determine the outcome.")
  private[commands] val progressInTermTitleFlag = flag("progress_in_terminal_title", false, "Show the command progress in the terminal title. Useful to see what Accio is doing when having multiple terminal tabs.")
  private[commands] val showTimestampFlag = flag("show_timestamps", false, "Include timestamps in messages")

  /**
   * Name of this command, as the user would type it.
   */
  def name: String

  /**
   * A short description, which appears in 'accio help'.
   */
  def help: String = ""

  /**
   * Specifies whether this command allows a residue after the parsed options.
   * For example, a command might expect a list of files to process in the residue.
   */
  def allowResidue: Boolean = false

  /**
   * Specifies whether the command should not be shown in the output of 'accio help'.
   */
  def hidden: Boolean = false

  final def terminalOptions = TerminalEventHandlerOptions(
    showProgressRateFlag(),
    showProgressFlag(),
    showTaskFinishFlag(),
    colorFlag(),
    cursesFlag(),
    quietFlag(),
    progressInTermTitleFlag(),
    showTimestampFlag())

  def execute(residue: Seq[String], env: CommandEnvironment): ExitCode
}