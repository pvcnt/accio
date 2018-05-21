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

import java.nio.file.Path

import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.{Http, Service, SimpleFilter}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Duration, Future, JavaTimer}

final class Webhook[T: Manifest](
  client: Service[Request, Response],
  path: String,
  headers: Map[String, String]) {

  /**
   * Send a request to the remote service. The returned future is successful only if a 200 HTTP
   * status code is returned. Otherwise it will be marked as failed with a [[WebhookException]].
   *
   * @param payload Payload to send.
   */
  def execute(payload: Any): Future[T] = {
    val request = Request(Method.Post, path)
    request.content = Webhook.objectMapper.writeValueAsBuf(payload)
    request.setContentTypeJson()
    request.headerMap ++= headers // Extra headers may override Content-Type.

    client(request).flatMap { resp =>
      if (resp.status != Status.Ok) {
        Future.exception(new WebhookException(resp))
      } else {
        Future(FinatraObjectMapper.parseResponseBody[T](resp, Webhook.objectMapper.reader[T]))
      }
    }
  }

  def close(): Unit = client.close()
}

object Webhook {
  private[webhook] val objectMapper = FinatraObjectMapper.create()
  private[this] val timer = new JavaTimer

  /**
   * Create a new webhook from a given YAML config file.
   *
   * @param path Path to the webhook configuration.
   * @tparam T Type of the response expected from the webhook.
   * @throws IllegalArgumentException If the provided path is not a readable file.
   */
  def fromFile[T: Manifest](path: Path): Webhook[T] = {
    val config = WebhookConfig.fromFile(path)
    val client = new TimeoutFilter(config.timeout).andThen(Http.client.newService(config.server))
    new Webhook(client, config.path, config.headers)
  }

  private class TimeoutFilter(timeout: Duration) extends SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      service(request).within(timer, timeout)
    }
  }

}