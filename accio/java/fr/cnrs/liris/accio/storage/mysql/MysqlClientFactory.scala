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

package fr.cnrs.liris.accio.storage.mysql

import com.twitter.conversions.time._
import com.twitter.finagle.client.DefaultPool
import com.twitter.finagle.mysql.Client
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle._
import com.twitter.util.{Throw, TimeoutException => UtilTimeoutException}

private[storage] object MysqlClientFactory {
  val responseClassifier: ResponseClassifier = ResponseClassifier.named("MysqlResponseClassifier") {
    case ReqRep(_, Throw(Failure(Some(_: TimeoutException)))) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(Failure(Some(_: UtilTimeoutException)))) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(_: TimeoutException)) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(_: UtilTimeoutException)) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(_: ChannelClosedException)) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(ChannelWriteException(Some(_: TimeoutException)))) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(ChannelWriteException(Some(_: UtilTimeoutException)))) => ResponseClass.RetryableFailure
    case ReqRep(_, Throw(ChannelWriteException(Some(_: ChannelClosedException)))) => ResponseClass.RetryableFailure
  }

  def apply(server: String, user: String, password: String, database: String): Client = {
    Mysql.client
      .withCredentials(user, password)
      .withDatabase(database)
      .withSessionQualifier.noFailFast
      .withSessionQualifier.noFailureAccrual
      .withResponseClassifier(responseClassifier)
      .configured(DefaultPool.Param(
        low = 0,
        high = 10,
        idleTime = 5.minutes,
        bufferSize = 0,
        maxWaiters = Int.MaxValue))
      .newRichClient(server)
  }
}