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

import java.io.FileInputStream
import java.nio.file.{Files, Path, Paths}
import java.util.Locale

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.util.FileUtils

import scala.util.control.NonFatal

/**
 * Parser for cluster configuration.
 *
 * @param mapper Finatra object mapper.
 */
final class ConfigParser(mapper: FinatraObjectMapper) {
  /**
   * Parse and merge cluster configurations from standard locations.
   *
   * @param productName Product name (e.g., accio).
   * @param reporter    Reporter.
   */
  def parse(productName: String, reporter: Reporter): ClusterConfig = {
    // We search for configuration files in two paths, a system-wide one and a user-defined one.
    // The system-wide path may be overriden by a environment variable.
    // The user-defined configuration has higher priority.
    val paths = Seq(
      FileUtils.expandPath(s"~/.$productName/clusters.json"),
      Paths.get(sys.env.getOrElse(s"${productName.toUpperCase(Locale.ROOT)}_CONFIG_ROOT", s"/etc/$productName")).resolve("clusters.json"))
    val configs = paths.filter(Files.isRegularFile(_)).map(parse(_, reporter))
    if (configs.isEmpty) {
      reporter.warn(s"No cluster configuration found (searched in ${paths.mkString(", ")}), assuming a local cluster")
      ClusterConfig.local
    } else {
      val merged = configs.flatten.groupBy(_.name).map { case (_, v) => v.head }
      if (merged.isEmpty) {
        reporter.warn(s"Cluster configuration is empty, assuming a local cluster")
        ClusterConfig.local
      } else {
        ClusterConfig(merged.toSeq)
      }
    }
  }

  /**
   * Parse a file into a cluster configuration.
   *
   * @param path     Path to a configuration file.
   * @param reporter Reporter.
   */
  def parse(path: Path, reporter: Reporter): Seq[ClusterConfig.Cluster] = {
    val fis = new FileInputStream(path.toFile)
    try {
      mapper.parse[Seq[ClusterConfig.Cluster]](fis)
    } catch {
      case NonFatal(e) =>
        reporter.error(s"Error while parsing cluster configuration in ${path.toAbsolutePath}: ${e.getMessage}")
        Seq.empty
    } finally {
      fis.close()
    }
  }
}

object ConfigParser {
  lazy val default = new ConfigParser(FinatraObjectMapper.create())
}