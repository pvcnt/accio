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

package fr.cnrs.liris.lumos.server

import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.domain.{LabelSelector, LumosException}
import fr.cnrs.liris.lumos.server.LumosService._
import fr.cnrs.liris.lumos.state.EventHandler
import fr.cnrs.liris.lumos.storage.{JobQuery, JobStore}

@Singleton
final class AgentServiceController @Inject()(jobStore: JobStore, eventHandler: EventHandler)
  extends Controller with LumosService.ServicePerEndpoint {

  override val getInfo = handle(GetInfo) { args: GetInfo.Args =>
    Future.value(GetInfoResponse("devel"))
  }

  override val pushEvent = handle(PushEvent) { args: PushEvent.Args =>
    eventHandler
      .handle(ThriftAdapter.toDomain(args.req.event))
      .map(_.toException)
      .flatMap {
        case Some(e) => Future.exception(e)
        case None => Future.value(PushEventResponse())
      }
  }

  override val getJob = handle(GetJob) { args: GetJob.Args =>
    jobStore
      .get(args.req.name)
      .flatMap {
        case Some(job) => Future.value(GetJobResponse(ThriftAdapter.toThrift(job)))
        case None => Future.exception(LumosException.NotFound(jobStore.resourceType, args.req.name))
      }
  }

  override val listJobs = handle(ListJobs) { args: ListJobs.Args =>
    val query = JobQuery(
      state = args.req.state.toSet.flatten.map(ThriftAdapter.toDomain),
      owner = args.req.owner,
      labels = args.req.labels.toSet.flatten.map(LabelSelector.parse))
    jobStore
      .list(query, limit = args.req.limit, offset = args.req.offset)
      .map { results =>
        ListJobsResponse(
          jobs = results.jobs.map(ThriftAdapter.toThrift),
          totalCount = results.totalCount)
      }
  }
}