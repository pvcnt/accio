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

package fr.cnrs.liris.accio.runtime.finagle

import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.{ConnectionFailedException, Failure}
import com.twitter.util.{Throw, Try}
import com.typesafe.scalalogging.StrictLogging

object RetryPolicies extends StrictLogging {
  def onFailure[Req, Res](backoffs: Stream[com.twitter.util.Duration]): RetryPolicy[(Req, Try[Res])] =
    RetryPolicy.backoff(backoffs) {
      case (_, Throw(f: Failure)) => f.cause.exists(isRetryable)
      case (_, Throw(t)) => isRetryable(t)
    }

  private def isRetryable(t: Throwable): Boolean = {
    val willRetry = t match {
      case _: ConnectionFailedException => true
      case _ => false
    }
    if (willRetry) {
      logger.info("Retrying. " + t.getMessage)
    }
    willRetry
  }
}