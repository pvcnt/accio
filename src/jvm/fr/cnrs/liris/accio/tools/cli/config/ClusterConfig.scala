/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.tools.cli.config

import com.twitter.inject.domain.WrappedValue

/**
 * Configuration specifying how to contact multiple Accio clusters.
 *
 * @param clusters Configuration of individual clusters.
 * @throws IllegalArgumentException If clusters configuration is invalid.
 */
case class ClusterConfig(clusters: Seq[Cluster]) extends WrappedValue[Seq[Cluster]] {
  {
    // Validate that cluster definitions are valid.
    require(clusters.nonEmpty, "You must define at least one cluster")
    val duplicateClusterNames = clusters.groupBy(_.name).filter(_._2.size > 1).keySet
    if (duplicateClusterNames.nonEmpty) {
      throw new IllegalArgumentException(s"Duplicate cluster names: ${duplicateClusterNames.mkString(", ")}")
    }
  }

  /**
   * Return the configuration of a given cluster.
   *
   * @param clusterName Cluster name.
   * @throws IllegalArgumentException If no such client exists.
   */
  @throws[IllegalArgumentException]
  def apply(clusterName: String): Cluster = {
    clusters.find(_.name == clusterName) match {
      case None => throw new IllegalArgumentException(s"No such cluster: $clusterName")
      case Some(clusterConfig) => clusterConfig
    }
  }

  /**
   * Return the configuration of the default cluster.
   */
  def defaultCluster: Cluster = clusters.head

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

/**
 * Configuration of a single Accio cluster. It specifies how the client should contact it.
 *
 * @param name Cluster name.
 * @param addr Cluster address (as a Finagle name).
 */
case class Cluster(name: String, addr: String)