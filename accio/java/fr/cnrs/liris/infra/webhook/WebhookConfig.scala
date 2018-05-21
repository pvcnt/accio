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

package fr.cnrs.liris.infra.webhook

import java.nio.file.{Files, Path}

import com.twitter.conversions.time._
import com.twitter.util.Duration

/**
 * Configuration of an HTTP webhook.
 *
 * @param server  Address of the server (may include a port number), e.g., "google.com:80".
 * @param path    Path of the endpoint to call (should start with a slash), e.g., "/callback".
 * @param headers Additional headers to include in the HTTP request.
 * @param timeout Maximum amount of time before the request fails.
 */
case class WebhookConfig(
  server: String,
  path: String = "/",
  headers: Map[String, String] = Map.empty,
  timeout: Duration = 5.seconds)

object WebhookConfig {
  /**
   * Parse a webhook configuration from YAML file.
   *
   * @param path Path to the webhook configuration.
   * @throws IllegalArgumentException If the provided path is not a readable file.
   */
  def fromFile(path: Path): WebhookConfig = {
    if (!path.toFile.isFile || !path.toFile.canRead) {
      throw new IllegalArgumentException(s"$path must be a readable file")
    }
    Webhook.objectMapper.parse(Files.readAllBytes(path))
  }
}