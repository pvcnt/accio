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

import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.tools.cli.config.ConfigParser

private[commands] trait ClientCommand {
  this: Command =>

  private[this] val clusterFlag = flag[String]("cluster", "Name of the cluster to use")
  private[this] val clientProvider = new ClientFactory(ConfigParser.default)

  protected final def client: AgentService.MethodPerEndpoint = {
    clusterFlag.get.map(clientProvider.apply).getOrElse(clientProvider.default)
  }
}