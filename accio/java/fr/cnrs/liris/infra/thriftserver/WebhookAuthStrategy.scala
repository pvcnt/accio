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

package fr.cnrs.liris.infra.thriftserver

import java.nio.file.Path

import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Future}
import fr.cnrs.liris.infra.webhook.Webhook
import fr.cnrs.liris.util.cache.CacheBuilder

final class WebhookAuthStrategy(webhook: Webhook[WebhookAuthStrategy.ReviewResponse], cacheTtl: Duration)
  extends AuthStrategy with Logging {

  private[this] val cache = CacheBuilder().expireAfterWrite(cacheTtl).build[String, Option[UserInfo]]

  override def authenticate(credentials: String): Future[Option[UserInfo]] = {
    cache.get(credentials) match {
      case Some(res) => Future.value(res)
      case None =>
        webhook
          .execute(WebhookAuthStrategy.ReviewRequest(credentials))
          .map { review =>
            if (review.authenticated) {
              Some(review.user.getOrElse(UserInfo(credentials)))
            } else {
              None
            }
          }
          .onSuccess { userInfo =>
            // We only cache successful responses.
            cache.put(credentials, userInfo)
          }
          .handle { case e: Throwable =>
            logger.error(s"Error in webhook: ${e.getMessage}")
            None
          }
    }
  }
}

object WebhookAuthStrategy {

  case class ReviewRequest(credentials: String)

  case class ReviewResponse(authenticated: Boolean, user: Option[UserInfo])

  /**
   * Create a new webhook authentication strategy from a given config file.
   *
   * @param path     Path to the webhook configuration.
   * @param cacheTtl Time to live for authentication results.
   * @throws IllegalArgumentException If the provided path is not a readable file.
   */
  def fromFile(path: Path, cacheTtl: Duration): WebhookAuthStrategy = {
    new WebhookAuthStrategy(Webhook.fromFile[ReviewResponse](path), cacheTtl)
  }
}