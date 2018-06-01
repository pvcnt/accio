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

import java.nio.file.Paths

import com.google.inject.{Provides, Singleton}
import com.twitter.conversions.time._
import com.twitter.inject.TwitterModule

import scala.collection.mutable

object AuthModule extends TwitterModule {
  private[this] val staticConfigFileFlag = flag[String](
    "auth.static.file",
    "File that contains the mapping between client identifiers and users.")
  private[this] val webhookConfigFileFlag = flag[String](
    "auth.webhook.config_file",
    "File that contains webhook configuration. The agent will query the remote service to " +
      "determine if authentication is allowed.")
  private[this] val webhookCacheTtlFlag = flag(
    "auth.webhook.cache_ttl",
    120.seconds,
    "The duration to cache responses from the webhook token strategy.")
  private[this] val trustFlag = flag(
    "auth.trust",
    false,
    "If enabled, the identity provided by clients is trusted without further verification.")
  private[this] val allowAnonymousFlag = flag(
    "auth.allow_anonymous",
    true,
    "Enables anonymous requests to the API server. Requests that are not allowed by another " +
      "authentication method are treated as anonymous requests. Anonymous requests have a " +
      "username of 'system:anonymous', and a group name of 'system:unauthenticated'.")

  @Provides
  @Singleton
  def providesAuthChain: AuthChain = {
    // The order of strategies is hard-coded and cannot be controlled by any flags. The rationale
    // is that they should be ordered cost (the cheapest strategies being evaluated first and the
    // most costly ones being evaluated last).
    val strategies = mutable.ListBuffer.empty[AuthStrategy]
    if (trustFlag()) {
      strategies += TrustAuthStrategy
    }
    staticConfigFileFlag.get.foreach { tokenFile =>
      strategies += StaticFileAuthStrategy.fromFile(Paths.get(tokenFile))
    }
    webhookConfigFileFlag.get.foreach { configFile =>
      strategies += WebhookAuthStrategy.fromFile(Paths.get(configFile), webhookCacheTtlFlag())
    }
    new AuthChain(strategies.toList, allowAnonymousFlag())
  }
}
