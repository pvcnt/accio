/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.scheduler

import com.google.inject.Singleton
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.{AgentService, AgentService$FinagleClient}

import scala.collection.mutable

@Singleton
class WorkerClientFactory {
  private[this] val clients = mutable.Map.empty[String, AgentService$FinagleClient]

  def create(dest: String): AgentService$FinagleClient = {
    clients.getOrElseUpdate(dest, {
      val service = Thrift.newService(dest)
      new AgentService.FinagledClient(service)
    })
  }

  def close(): Unit = clients.values.foreach(_.service.close())
}