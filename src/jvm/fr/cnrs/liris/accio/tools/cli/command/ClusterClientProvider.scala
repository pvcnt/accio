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

package fr.cnrs.liris.accio.tools.cli.command

import java.nio.file.Paths
import javax.annotation.concurrent.NotThreadSafe

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}
import fr.cnrs.liris.accio.tools.cli.config.{Cluster, ClusterConfig, ConfigParser, InvalidConfigException}
import fr.cnrs.liris.common.util.FileUtils

import scala.collection.mutable

/**
 * Provide Finagle clients to communicate with Accio clusters. It behaves as a clients pool, clients being reused
 * between several calls. It reads on-the-fly cluster configurations files when needed (and only when needed).
 *
 * The `ACCIO_CONFIG_ROOT` environment variable is used to locate a system-wide configuration file.
 *
 * @param parser Cluster configuration parser.
 */
@NotThreadSafe
@Singleton
class ClusterClientProvider @Inject()(parser: ConfigParser) {
  private[this] val clients = mutable.Map.empty[String, AgentService$FinagleClient]
  private[this] lazy val config = parseConfig

  /**
   * Return a Finagle client for a given cluster, or the default one.
   *
   * @param clusterName Cluster name.
   * @throws IllegalArgumentException If given cluster does not exists.
   * @throws RuntimeException         If no clusters configuration file was found.
   * @throws InvalidConfigException   If clusters configuration is invalid.
   */
  @throws[InvalidConfigException]
  def apply(clusterName: Option[String]): AgentService$FinagleClient =
  clusterName match {
    case None => getOrCreate(config.defaultCluster)
    case Some(name) => getOrCreate(config(name))
  }

  /**
   * Return a Finagle client for the default cluster.
   *
   * @throws RuntimeException       If no clusters configuration file was found.
   * @throws InvalidConfigException If clusters configuration is invalid.
   */
  @throws[InvalidConfigException]
  def apply(): AgentService$FinagleClient = apply(None)

  /**
   * Return a Finagle client for a given cluster.
   *
   * @param clusterName Cluster name.
   * @throws IllegalArgumentException If given cluster does not exists.
   * @throws RuntimeException         If no clusters configuration file was found.
   * @throws InvalidConfigException   If clusters configuration is invalid.
   */
  @throws[InvalidConfigException]
  def apply(clusterName: String): AgentService$FinagleClient = apply(Some(clusterName))

  /**
   * Close all clients.
   */
  def close(): Unit = clients.values.foreach(_.service.close())

  /**
   * Return an existing client for a given cluster, if any, or create a new one and memoize it.
   *
   * @param config Cluster.
   */
  private def getOrCreate(config: Cluster) = {
    clients.getOrElseUpdate(config.name, {
      val service = Thrift.newService(config.addr)
      new AgentService.FinagledClient(service)
    })
  }

  /**
   * Parse and merge clusters configuration.
   */
  private def parseConfig: ClusterConfig = {
    // We search for configuration files in two paths, a system-wide one and a user-defined one.
    val paths = Seq(
      Paths.get(sys.env.getOrElse("ACCIO_CONFIG_ROOT", "/etc/accio")).resolve("clusters.json"),
      FileUtils.expandPath("~/.accio/clusters.json"))
    val configs = paths.filter(_.toFile.exists).map(parser.parse)
    if (configs.isEmpty) {
      throw new RuntimeException(s"No clusters configuration in paths:\n  ${paths.map(_.toAbsolutePath).mkString("\n  ")}")
    }
    configs.reduce(_.merge(_))
  }
}
