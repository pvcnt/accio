/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the ter ms of the GNU General Public License as published by
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

package fr.cnrs.liris.lumos.server

import com.twitter.finatra.thrift.routing.ThriftWarmup
import com.twitter.inject.utils.Handler
import com.twitter.util.logging.Logging
import com.twitter.util.{Return, Throw}
import fr.cnrs.liris.infra.thriftserver.AuthFilter
import fr.cnrs.liris.lumos.server.LumosService.ListJobs
import javax.inject.Inject

private[server] final class ServerWarmupHandler @Inject()(warmup: ThriftWarmup)
  extends Handler with Logging {

  override def handle(): Unit = {
    try {
      AuthFilter.MasterClientId.asCurrent {
        warmup.send(ListJobs, ListJobs.Args(ListJobsRequest()), times = 3) {
          case Return(_) => // Warmup request was successful.
          case Throw(e) => logger.warn("Warmup request failed", e)
        }
      }
    } catch {
      case e: Throwable =>
        // Here we don't want a warmup failure to prevent server start-up --
        // this is important if your service will call downstream services
        // during warmup that could be temporarily down or unavailable.
        // We don't want that unavailability to cause our server to fail
        // warm-up and prevent the server from starting. So we simply log
        // the error message here.
        logger.error(e.getMessage, e)
    }
    logger.info("Warm-up done.")
  }
}