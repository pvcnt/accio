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
 * Configuration specifying how to contact multiple Accio clusters.
 *
 * @param clusters Configuration of individual clusters.
 * @throws IllegalArgumentException If clusters configuration is invalid.
 */
case class ClusterConfig(clusters: Seq[ClusterConfig.Cluster]) {
  /**
   * Return the configuration of a given cluster.
   *
   * @param clusterName Cluster name.
   * @throws IllegalArgumentException If no such client exists.
   */
  @throws[IllegalArgumentException]
  def apply(clusterName: String): ClusterConfig.Cluster = {
    clusters.find(_.name == clusterName) match {
      case None => throw new IllegalArgumentException(s"No such cluster: $clusterName")
      case Some(clusterConfig) => clusterConfig
    }
  }

  /**
   * Return the configuration of the default cluster.
   */
  def defaultCluster: ClusterConfig.Cluster = clusters.head

  /**
   * Merge two cluster configurations into a new one. Entries in the other configuration for a cluster that already
   * exists will overwrite existing configuration, other will be added. Order remains unchanged.
   *
   * @param other Other clusters configuration.
   */
  def merge(other: ClusterConfig): ClusterConfig = {
    var mergedClusters = clusters
    other.clusters.foreach { otherCluster =>
      mergedClusters.find(_.name == otherCluster.name) match {
        case None => mergedClusters ++= Seq(otherCluster)
        case Some(cluster) => mergedClusters = mergedClusters.updated(mergedClusters.indexOf(cluster), otherCluster)
      }
    }
    ClusterConfig(mergedClusters)
  }
}

object ClusterConfig {
  /**
   * Return the configuration for a local cluster listening on the default port.
   */
  def local = ClusterConfig(Seq(Cluster("default", "localhost:9999")))

  /**
   * Definition of a single cluster.
   *
   * @param name        Name of the cluster (used by the client to reference it).
   * @param server      Address to the agent server (specified as a Finagle name).
   * @param credentials Credentials used to authenticate against the server.
   */
  case class Cluster(name: String, server: String, credentials: Option[String] = None)

}