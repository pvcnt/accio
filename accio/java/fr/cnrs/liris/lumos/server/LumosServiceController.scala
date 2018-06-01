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
import fr.cnrs.liris.infra.thriftserver.ServerException
import fr.cnrs.liris.lumos.domain.thrift.ThriftAdapter
import fr.cnrs.liris.lumos.domain.{LabelSelector, Status}
import fr.cnrs.liris.lumos.server.LumosService._
import fr.cnrs.liris.lumos.state.EventHandler
import fr.cnrs.liris.lumos.storage.{JobQuery, JobStore}
import fr.cnrs.liris.lumos.version.Version

@Singleton
private[server] final class LumosServiceController @Inject()(store: JobStore, eventHandler: EventHandler)
  extends Controller with LumosService.ServicePerEndpoint {

  //TODO: garbage collect lost jobs.

  override val getInfo = handle(GetInfo) { args: GetInfo.Args =>
    Future.value(GetInfoResponse(Version.Current.toString))
  }

  override val pushEvent = handle(PushEvent) { args: PushEvent.Args =>
    val event = ThriftAdapter.toDomain(args.req.event)
    eventHandler.handle(event).map(toException).flatMap {
      case Some(e) => Future.exception(e)
      case None => Future.value(PushEventResponse())
    }
  }

  override val getJob = handle(GetJob) { args: GetJob.Args =>
    store.get(args.req.name).flatMap {
      case Some(job) =>Future.value(GetJobResponse(ThriftAdapter.toThrift(job)))
      case None => Future.exception(ServerException.NotFound("jobs", args.req.name))
    }
  }

  override val listJobs = handle(ListJobs) { args: ListJobs.Args =>
    val maybeSelector = args.req.labels.map(LabelSelector.parse)
    if (maybeSelector.exists(_.isLeft)) {
      Future.exception(ServerException.InvalidArgument(Seq(ServerException.FieldViolation(s"Cannot parse: ${args.req.labels}", "labels"))))
    } else {
      val query = JobQuery(
        state = args.req.state.toSet.flatten.map(ThriftAdapter.toDomain),
        owner = args.req.owner,
        labels = maybeSelector.map(_.right.get))
      store
        .list(query, limit = args.req.limit, offset = args.req.offset)
        .map { results =>
          ListJobsResponse(
            jobs = results.jobs.map(ThriftAdapter.toThrift),
            totalCount = results.totalCount)
        }
    }
  }

  private def toException(status: Status): Option[ServerException] =
    status match {
      case Status.Ok => None
      case Status.AlreadyExists(jobName) => Some(ServerException.AlreadyExists("jobs", jobName))
      case Status.NotFound(jobName) => Some(ServerException.NotFound("jobs", jobName))
      case Status.InvalidArgument(errors) =>
        Some(ServerException.InvalidArgument(errors.map(v => ServerException.FieldViolation(v.message, v.field))))
      case Status.FailedPrecondition(jobName, errors) =>
        Some(ServerException.FailedPrecondition("jobs", jobName, errors.map(v => ServerException.FieldViolation(v.message, v.field))))
    }
}