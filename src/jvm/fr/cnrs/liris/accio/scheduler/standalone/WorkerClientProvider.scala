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

package fr.cnrs.liris.accio.scheduler.standalone

import java.util.concurrent.ConcurrentHashMap

import com.google.inject.Singleton
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}

import scala.collection.JavaConverters._

/**
 * Create clients to communicate with workers. Clients are memoized, avoiding to re-create one each time.
 */
@Singleton
final class WorkerClientProvider {
  private[this] val clients = new ConcurrentHashMap[String, AgentService$FinagleClient]().asScala

  /**
   *
   * @param dest
   * @return
   */
  def apply(dest: String): AgentService$FinagleClient = {
    // - We do not want to fail-fast, because there will always be only one reachable host.
    clients.getOrElseUpdate(dest, {
      val service = Thrift.client
        .withSessionQualifier.noFailFast
        .newService(dest)
      new AgentService.FinagledClient(service)
    })
  }

  def close(): Unit = clients.values.foreach(_.service.close())
}