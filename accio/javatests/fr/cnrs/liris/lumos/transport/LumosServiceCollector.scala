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

package fr.cnrs.liris.lumos.transport

import com.twitter.finagle.Service
import com.twitter.util.{Duration, Future}
import fr.cnrs.liris.lumos.domain.thrift
import fr.cnrs.liris.lumos.server.LumosService.{GetInfo, GetJob, ListJobs, PushEvent}
import fr.cnrs.liris.lumos.server._

import scala.collection.mutable

private[transport] class LumosServiceCollector(latency: Duration) extends LumosService.ServicePerEndpoint {
  private[this] val collector = mutable.ListBuffer.empty[thrift.Event]

  def eventsOf(parent: String): Seq[thrift.Event] = collector.filter(_.parent == parent)

  override def pushEvent: Service[PushEvent.Args, PushEventResponse] = new Service[PushEvent.Args, PushEventResponse] {
    override def apply(request: PushEvent.Args): Future[PushEventResponse] = synchronized {
      Thread.sleep(latency.inMillis)
      collector += request.req.event
      Future.value(PushEventResponse())
    }
  }

  override def getInfo: Service[GetInfo.Args, GetInfoResponse] = ???

  override def getJob: Service[GetJob.Args, GetJobResponse] = ???

  override def listJobs: Service[ListJobs.Args, ListJobsResponse] = ???
}