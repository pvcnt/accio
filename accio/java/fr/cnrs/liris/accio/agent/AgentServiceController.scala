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

package fr.cnrs.liris.accio.agent

import com.google.common.eventbus.EventBus
import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.AgentService._
import fr.cnrs.liris.accio.api._
import fr.cnrs.liris.accio.scheduler.{Process, Scheduler}
import fr.cnrs.liris.accio.state.StateManager
import fr.cnrs.liris.accio.storage.{JobStore, Storage}
import fr.cnrs.liris.accio.version.Version

@Singleton
final class AgentServiceController @Inject()(
  jobFactory: JobFactory,
  jobPreparator: JobPreparator,
  jobValidator: JobValidator,
  stateManager: StateManager,
  storage: Storage,
  scheduler: Scheduler,
  opRegistry: OpRegistry,
  eventBus: EventBus)
  extends Controller with AgentService.ServicePerEndpoint {

  override val getCluster = handle(GetCluster) { args: GetCluster.Args =>
    Future {
      GetClusterResponse(Version.Current.toString)
    }
  }

  override val getOperator = handle(GetOperator) { args: GetOperator.Args =>
    Future {
      opRegistry.get(args.req.name) match {
        case None => throw Errors.notFound("operator", args.req.name)
        case Some(opDef) => GetOperatorResponse(opDef)
      }
    }
  }

  override val listOperators = handle(ListOperators) { args: ListOperators.Args =>
    Future {
      var results = opRegistry.ops.toSeq
      if (!args.req.includeDeprecated) {
        results = results.filter(_.deprecation.isEmpty)
      }
      ListOperatorsResponse(results)
    }
  }

  override val validateJob = handle(ValidateJob) { args: ValidateJob.Args =>
    Future {
      val job = jobPreparator.prepare(args.req.job, UserInfo.current)
      val result = jobValidator.validate(job)
      ValidateJobResponse(result.errors, result.warnings)
    }
  }

  override val createJob = handle(CreateJob) { args: CreateJob.Args =>
    val job = jobPreparator.prepare(args.req.job, UserInfo.current)
    val result = jobValidator.validate(job)
    if (result.isInvalid) {
      Future.exception(Errors.badRequest("job", result.errors, result.warnings))
    } else {
      val jobs = jobFactory.create(args.req.job)
      storage.jobs
        .create(jobs.head)
        .flatMap {
          case true =>
            Future.join(jobs.tail.map(storage.jobs.create))
              .ensure(eventBus.post(JobCreatedEvent(jobs.head.name)))
              .map(_ => CreateJobResponse(jobs.head, result.warnings))
          case false =>
            // It may happen if the user manually specified a job name that clashes with an
            // existing one, or if we are unlucky when generating the random job name.
            throw Errors.alreadyExists("job", jobs.head.name)
        }
    }
  }

  override val getJob = handle(GetJob) { args: GetJob.Args =>
    storage.jobs
      .get(args.req.name)
      .flatMap {
        case None => Future.exception(Errors.notFound("job", args.req.name))
        case Some(job) => Future.value(GetJobResponse(job))
      }
  }

  override val listJobs = handle(ListJobs) { args: ListJobs.Args =>
    val query = JobStore.Query(
      title = args.req.title,
      author = args.req.author,
      state = args.req.state.map(_.toSet),
      parent = args.req.parent,
      tags = args.req.tags.toSet.flatten,
      q = args.req.q)
    storage.jobs
      .list(query, limit = args.req.limit, offset = args.req.offset)
      .map { results =>
        val jobs = results.results.map(job => job.copy(status = job.status.copy(tasks = None)))
        ListJobsResponse(jobs, results.totalCount)
      }
  }

  override val deleteJob = handle(DeleteJob) { args: DeleteJob.Args =>
    storage.jobs
      .get(args.req.name)
      .flatMap {
        case None => Future.exception(Errors.notFound("job", args.req.name))
        case Some(job) =>
          val f = if (job.status.children.isDefined) {
            storage.jobs
              .list(JobStore.Query(parent = Some(job.name)))
              .flatMap { children =>
                Future.join(children.results.map(child => stateManager.delete(child, Some(job))))
              }
              .map(_ => storage.jobs.delete(job.name))
          } else if (job.parent.isEmpty) {
            stateManager.delete(job, None)
          } else {
            Future.exception(Errors.failedPrecondition("job", args.req.name, "A child job cannot be deleted"))
          }
          f.flatMap {
            case true => Future.value(DeleteJobResponse())
            case false => Future.exception(Errors.notFound("job", args.req.name))
          }
      }
  }

  override val killJob = handle(KillJob) { args: KillJob.Args =>
    storage.jobs
      .get(args.req.name)
      .flatMap {
        case None => Future.exception(Errors.notFound("job", args.req.name))
        case Some(job) =>
          if (job.status.children.isDefined) {
            storage.jobs.list(JobStore.Query(parent = Some(job.name))).flatMap { children =>
              Future.join(children.results.map(child => stateManager.kill(child, Some(job))))
            }
          } else {
            job.parent match {
              case None => stateManager.kill(job, None)
              case Some(parentName) =>
                storage.jobs
                  .get(parentName)
                  .flatMap(parent => stateManager.kill(job, parent))
            }
          }
      }
      .map(_ => KillJobResponse())
  }

  override val listLogs = handle(ListLogs) { args: ListLogs.Args =>
    storage.jobs
      .get(args.req.job)
      .flatMap {
        case None => Future.exception(Errors.notFound("job", args.req.job))
        case Some(job) =>
          job.status.tasks.toSeq.flatten.find(_.name == args.req.step) match {
            case None => Future.exception(Errors.notFound("task", s"${args.req.job}/${args.req.step}"))
            case Some(task) =>
              val processName = Process.name(args.req.job, task.name)
              scheduler.getLogs(processName, args.req.kind, skip = args.req.skip, tail = args.req.tail)
          }
      }
      .map(lines => ListLogsResponse(lines))
  }
}