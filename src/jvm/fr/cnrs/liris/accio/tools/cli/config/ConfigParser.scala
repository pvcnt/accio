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

package fr.cnrs.liris.accio.tools.cli.config

import java.io.{FileInputStream, IOException}
import java.nio.file.Path

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.twitter.finatra.json.FinatraObjectMapper

/**
 * Exception thrown when a configuration is invalid, either syntaxically or semantically.
 *
 * @param uri     Configuration file this exception is about.
 * @param message Error message.
 * @param cause   Possible root cause.
 */
class InvalidConfigException(uri: String, message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Parser for client configuration.
 *
 * @param mapper Finatra object mapper.
 */
class ConfigParser(mapper: FinatraObjectMapper) {
  /**
   * Parse a file into a client configuration.
   *
   * @param path Path to a configuration file.
   * @throws InvalidConfigException If configuration is invalid
   */
  @throws[InvalidConfigException]
  def parse(path: Path): ClusterConfig = {
    val fis = new FileInputStream(path.toFile)
    val config = try {
      // It did not go well when trying to deserialized directly as ClusterConfig, despite it being a WrappedValue.
      // So we fall back to deserializing clusters directly.
      ClusterConfig(mapper.parse[Seq[Cluster]](fis))
    } catch {
      case e@(_: IOException | _: JsonParseException | _: JsonMappingException) =>
        throw new InvalidConfigException(path.toAbsolutePath.toString, s"Error while parsing cluster configuration", e)
      case e: IllegalArgumentException =>
        throw new InvalidConfigException(path.toAbsolutePath.toString, e.getMessage.stripPrefix("requirement failed: "))
    } finally {
      fis.close()
    }
    config
  }
}