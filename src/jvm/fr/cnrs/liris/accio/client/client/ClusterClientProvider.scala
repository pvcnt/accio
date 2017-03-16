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

package fr.cnrs.liris.accio.client.client

import java.nio.file.{Path, Paths}

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.mutable

/**
 * Provide Finagle clients to contact Accio clusters.
 *
 * @param parser Cluster configuration parser.
 */
@Singleton
class ClusterClientProvider @Inject()(parser: ClusterConfigParser) {
  private[this] val clients = mutable.Map.empty[String, AgentService$FinagleClient]
  private[this] lazy val config = {
    val paths = Seq(
      Paths.get(sys.env.getOrElse("ACCIO_CONFIG_ROOT", "/etc/accio/clusters.json")),
      FileUtils.expandPath("~/.accio/clusters.json"))
    val configs = paths.filter(_.toFile.exists).map(parser.parse)
    if (configs.isEmpty) {
      throw new InvalidClusterConfigException("No Accio clusters configuration file")
    }
    configs.reduce(_.merge(_))
  }

  def apply(clusterName: Option[String]): AgentService$FinagleClient =
    clusterName match {
      case None => getOrCreate(config.defaultCluster)
      case Some(name) => getOrCreate(config(name))
    }

  /**
   * Return a Finagle client for the default cluster.
   */
  def apply(): AgentService$FinagleClient = apply(None)

  /**
   * Return a Finagle client for a specific cluster.
   *
   * @param clusterName Cluster name.
   * @throws IllegalArgumentException If no such cluster exists.
   */
  @throws[IllegalArgumentException]
  def apply(clusterName: String): AgentService$FinagleClient = apply(Some(clusterName))

  /**
   * Close all clients.
   */
  def close(): Unit = clients.values.foreach(_.service.close())

  private def getOrCreate(config: Cluster) = {
    clients.getOrElseUpdate(config.name, {
      val service = Thrift.newService(config.addr)
      new AgentService.FinagledClient(service)
    })
  }
}
