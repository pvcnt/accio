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
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.RunCreatedEvent
import fr.cnrs.liris.accio.api.thrift.{InvalidSpecException, InvalidSpecMessage, UnknownRunException}
import fr.cnrs.liris.accio.config.ClusterName
import fr.cnrs.liris.accio.dsl.{RunParser, WorkflowParser}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.service.{OpRegistry, RunFactory, RunManager, WorkflowFactory}
import fr.cnrs.liris.accio.storage.{RunQuery, Storage, WorkflowQuery}
import fr.cnrs.liris.accio.util.Version

import scala.collection.mutable

@Singleton
final class AgentServiceImpl @Inject()(
  runFactory: RunFactory,
  workflowFactory: WorkflowFactory,
  runParser: RunParser,
  workflowParser: WorkflowParser,
  runManager: RunManager,
  storage: Storage,
  scheduler: Scheduler,
  opRegistry: OpRegistry,
  eventBus: EventBus,
  @ClusterName clusterName: String)
  extends AgentService[Future] with Logging {

  override def getCluster(req: GetClusterRequest): Future[GetClusterResponse] = Future {
    GetClusterResponse(clusterName, Version.Current.toString)
  }

  override def getOperator(req: GetOperatorRequest): Future[GetOperatorResponse] = Future {
    GetOperatorResponse(opRegistry.get(req.name))
  }

  override def listOperators(req: ListOperatorsRequest): Future[ListOperatorsResponse] = Future {
    var results = opRegistry.ops.toSeq
    if (!req.includeDeprecated) {
      results = results.filter(_.deprecation.isEmpty)
    }
    ListOperatorsResponse(results)
  }

  override def parseWorkflow(req: ParseWorkflowRequest): Future[ParseWorkflowResponse] = Future {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    try {
      val workflow = workflowParser.parse(req.content, req.filename, warnings)
      ParseWorkflowResponse(workflow = Some(workflow), warnings = warnings.toSeq)
    } catch {
      case e: InvalidSpecException => ParseWorkflowResponse(errors = e.errors, warnings = e.warnings)
    }
  }

  override def pushWorkflow(req: PushWorkflowRequest): Future[PushWorkflowResponse] = Future {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    val workflow = workflowFactory.create(req.spec, req.user, warnings)
    storage.write(_.workflows.save(workflow))
    PushWorkflowResponse(workflow, warnings.toSeq)
  }

  override def getWorkflow(req: GetWorkflowRequest): Future[GetWorkflowResponse] = Future {
    val workflow = req.version match {
      case Some(version) => storage.read(_.workflows.get(req.id, version))
      case None => storage.read(_.workflows.get(req.id))
    }
    GetWorkflowResponse(workflow)
  }

  override def listWorkflows(req: ListWorkflowsRequest): Future[ListWorkflowsResponse] = Future {
    val query = WorkflowQuery(
      name = req.name,
      owner = req.owner,
      q = req.q,
      limit = req.limit,
      offset = req.offset)
    val workflows = storage.read(_.workflows.list(query))
    ListWorkflowsResponse(workflows.results, workflows.totalCount)
  }

  override def parseRun(req: ParseRunRequest): Future[ParseRunResponse] = Future {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    try {
      val run = runParser.parse(req.content, req.params.toMap, warnings)
      ParseRunResponse(run = Some(run), warnings = warnings.toSeq)
    } catch {
      case e: InvalidSpecException => ParseRunResponse(errors = e.errors, warnings = e.warnings)
    }
  }

  override def createRun(req: CreateRunRequest): Future[CreateRunResponse] = Future {
    val warnings = mutable.Set.empty[InvalidSpecMessage]
    val runs = runFactory.create(req.spec, req.user, warnings)
    storage.write(stores => runs.foreach(stores.runs.save))
    if (runs.nonEmpty) {
      // Maybe it could happen that an unfortunate parameter sweep generate no run...
      eventBus.post(RunCreatedEvent(runs.head.id, runs.tail.map(_.id)))
    }
    CreateRunResponse(runs.map(_.id), warnings.toSeq)
  }

  override def getRun(req: GetRunRequest): Future[GetRunResponse] = Future {
    val run = storage.read(_.runs.get(req.id))
    GetRunResponse(run)
  }

  override def listRuns(req: ListRunsRequest): Future[ListRunsResponse] = Future {
    val query = RunQuery(
      name = req.name,
      owner = req.owner,
      workflow = req.workflowId,
      status = req.status.toSet,
      parent = req.parent,
      clonedFrom = req.clonedFrom,
      tags = req.tags.toSet,
      q = req.q,
      limit = req.limit,
      offset = req.offset)
    val runs = storage.read(_.runs.list(query))
    ListRunsResponse(runs.results, runs.totalCount)
  }

  override def deleteRun(req: DeleteRunRequest): Future[DeleteRunResponse] = Future {
    storage.write { stores =>
      stores.runs.get(req.id) match {
        case None => throw UnknownRunException(req.id)
        case Some(run) =>
          if (run.children.nonEmpty) {
            // It is a parent run, cancel and delete child all runs.
            run.children.foreach(scheduler.kill)
            run.children.foreach(stores.runs.remove)
          } else if (run.parent.isDefined) {
            // It is a child run, update or delete parent run.
            stores.runs.get(run.parent.get).foreach { parent =>
              if (parent.children.size > 1) {
                // There are several child runs left, remove current one from the list.
                stores.runs.save(parent.copy(children = parent.children.filterNot(_ == run.id)))
              } else {
                // It was the last child of this run, delete it as it is now useless.
                stores.runs.remove(parent.id)
              }
            }
          } else {
            // It is a single run, cancel it.
            scheduler.kill(run.id)
          }
          // Finally, delete the run.
          stores.runs.remove(run.id)
      }
      DeleteRunResponse()
    }
  }

  override def killRun(req: KillRunRequest): Future[KillRunResponse] = Future {
    storage.write { stores =>
      val newRun = stores.runs.get(req.id) match {
        case None => throw UnknownRunException(req.id)
        case Some(run) =>
          if (run.children.nonEmpty) {
            var newParent = run
            // If is a parent run, kill child all runs.
            run.children.foreach { childId =>
              stores.runs.get(childId).foreach { child =>
                val killedTasks = scheduler.kill(child.id)
                val res = runManager.onKill(child, killedTasks.map(_.nodeName), Some(run))
                stores.runs.save(res._1)
                res._2.foreach(p => newParent = p)
              }
            }
            stores.runs.save(newParent)
            newParent
          } else {
            run.parent match {
              case None =>
                val killedTasks = scheduler.kill(run.id)
                val (newRun, _) = runManager.onKill(run, killedTasks.map(_.nodeName), None)
                stores.runs.save(newRun)
                newRun
              case Some(parentId) =>
                var newRun = run
                stores.runs.get(parentId).foreach { parent =>
                  val killedTasks = scheduler.kill(run.id)
                  val res = runManager.onKill(run, killedTasks.map(_.nodeName), Some(parent))
                  newRun = res._1
                  stores.runs.save(newRun)
                  res._2.foreach(stores.runs.save)
                }
                newRun
            }
          }
      }
      KillRunResponse(newRun)
    }
  }

  override def updateRun(req: UpdateRunRequest): Future[UpdateRunResponse] = Future {
    storage.write { stores =>
      stores.runs.get(req.id) match {
        case None => throw UnknownRunException(req.id)
        case Some(run) =>
          var newRun = run.parent.flatMap(stores.runs.get).getOrElse(run)
          req.name.foreach(name => newRun = newRun.copy(name = Some(name)))
          req.notes.foreach(notes => newRun = newRun.copy(notes = Some(notes)))
          if (req.tags.nonEmpty) {
            newRun = newRun.copy(tags = req.tags)
          }
          stores.runs.save(newRun)
          UpdateRunResponse(newRun)
      }
    }
  }

  override def listLogs(req: ListLogsRequest): Future[ListLogsResponse] = Future {
    val results = storage.read(_.runs.get(req.runId))
      .flatMap(_.state.nodes.find(_.name == req.nodeName))
      .flatMap(_.taskId)
      .map(taskId => scheduler.getLogs(taskId, req.kind, skip = req.skip, tail = req.tail))
      .getOrElse(Seq.empty)
    ListLogsResponse(results)
  }
}
