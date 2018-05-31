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

import com.twitter.app.Flags
import com.twitter.finagle.thrift.{ClientId, ThriftClientRequest}
import com.twitter.finagle.{Service, Thrift}
import com.twitter.util.Future

/**
 * A command that is part of a command-line application.
 */
trait Command {
  val flag = new Flags("accio", includeGlobal = false, failFastUntilParsed = true)
  private[this] val useColor = flag("color", true, "Use terminal controls to colorize output.")
  private[this] val quiet = flag("quiet", false, "Suppress all output, even for errors. Use exit code to determine the outcome.")
  private[this] val showTimestamp = flag("show_timestamps", false, "Include timestamps in messages")

  private[this] val clusterFlag = flag[String]("cluster", "Name of the cluster to use")

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

  def execute(residue: Seq[String], env: Environment): Future[ExitCode]

  final def createThriftClient(env: Environment): Service[ThriftClientRequest, Array[Byte]] = {
    val cluster = clusterFlag.get.map(env.config.apply).getOrElse(env.config.defaultCluster)
    var builder = Thrift.client
      .withSessionQualifier.noFailFast
      .withSessionQualifier.noFailureAccrual
    cluster.credentials.foreach(credentials => builder = builder.withClientId(ClientId(credentials)))
    builder.newService(cluster.server)
  }

  final def terminalOptions: AnsiTerminalReporter.Options = {
    AnsiTerminalReporter.Options(
      useColor = useColor(),
      showTimestamp = showTimestamp())
  }
}