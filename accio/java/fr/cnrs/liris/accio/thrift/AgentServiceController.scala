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

package fr.cnrs.liris.accio.thrift

import com.google.common.eventbus.EventBus
import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import fr.cnrs.liris.accio.agent.AgentService._
import fr.cnrs.liris.accio.agent._
import fr.cnrs.liris.accio.api.RunCreatedEvent
import fr.cnrs.liris.accio.api.thrift.{InvalidSpecException, InvalidSpecMessage, UnknownRunException}
import fr.cnrs.liris.accio.config.ClusterName
import fr.cnrs.liris.accio.dsl.{RunParser, WorkflowParser}
import fr.cnrs.liris.accio.scheduler.Scheduler
import fr.cnrs.liris.accio.service.{OpRegistry, RunFactory, RunManager, WorkflowFactory}
import fr.cnrs.liris.accio.storage.{RunQuery, Storage, WorkflowQuery}
import fr.cnrs.liris.accio.version.Version

import scala.collection.mutable

@Singleton
final class AgentServiceController @Inject()(
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
  extends Controller with AgentService.ServicePerEndpoint {

  override val getCluster = handle(GetCluster) { args: GetCluster.Args =>
    Future {
      GetClusterResponse(clusterName, Version.Current.toString)
    }
  }

  override val getOperator = handle(GetOperator) { args: GetOperator.Args =>
    Future {
      GetOperatorResponse(opRegistry.get(args.req.name))
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

  override val parseWorkflow = handle(ParseWorkflow) { args: ParseWorkflow.Args =>
    Future {
      val warnings = mutable.Set.empty[InvalidSpecMessage]
      try {
        val workflow = workflowParser.parse(args.req.content, args.req.filename, warnings)
        ParseWorkflowResponse(workflow = Some(workflow), warnings = warnings.toSeq)
      } catch {
        case e: InvalidSpecException => ParseWorkflowResponse(errors = e.errors, warnings = e.warnings)
      }
    }
  }

  override val pushWorkflow = handle(PushWorkflow) { args: PushWorkflow.Args =>
    Future {
      val warnings = mutable.Set.empty[InvalidSpecMessage]
      val workflow = workflowFactory.create(args.req.spec, args.req.user, warnings)
      storage.write(_.workflows.save(workflow))
      PushWorkflowResponse(workflow, warnings.toSeq)
    }
  }

  override val getWorkflow = handle(GetWorkflow) { args: GetWorkflow.Args =>
    Future {
      val workflow = args.req.version match {
        case Some(version) => storage.read(_.workflows.get(args.req.id, version))
        case None => storage.read(_.workflows.get(args.req.id))
      }
      GetWorkflowResponse(workflow)
    }
  }

  override val listWorkflows = handle(ListWorkflows) { args: ListWorkflows.Args =>
    Future {
      val query = WorkflowQuery(
        name = args.req.name,
        owner = args.req.owner,
        q = args.req.q,
        limit = args.req.limit,
        offset = args.req.offset)
      var workflows = storage.read(_.workflows.list(query))
      workflows = workflows.copy(results = workflows.results.map(workflow => workflow.copy(graph = workflow.graph.unsetNodes)))
      ListWorkflowsResponse(workflows.results, workflows.totalCount)
    }
  }

  override val parseRun = handle(ParseRun) { args: ParseRun.Args =>
    Future {
      val warnings = mutable.Set.empty[InvalidSpecMessage]
      try {
        val run = runParser.parse(args.req.content, args.req.params.toMap, warnings)
        ParseRunResponse(run = Some(run), warnings = warnings.toSeq)
      } catch {
        case e: InvalidSpecException => ParseRunResponse(errors = e.errors, warnings = e.warnings)
      }
    }
  }

  override val createRun = handle(CreateRun) { args: CreateRun.Args =>
    Future {
      val warnings = mutable.Set.empty[InvalidSpecMessage]
      val runs = runFactory.create(args.req.spec, args.req.user, warnings)
      storage.write(stores => runs.foreach(stores.runs.save))
      if (runs.nonEmpty) {
        // Maybe it could happen that an unfortunate parameter sweep generate no run...
        eventBus.post(RunCreatedEvent(runs.head.id, runs.tail.map(_.id)))
      }
      CreateRunResponse(runs.map(_.id), warnings.toSeq)
    }
  }

  override val getRun = handle(GetRun) { args: GetRun.Args =>
    Future {
      val run = storage.read(_.runs.get(args.req.id))
      GetRunResponse(run)
    }
  }

  override val listRuns = handle(ListRuns) { args: ListRuns.Args =>
    Future {
      val query = RunQuery(
        name = args.req.name,
        owner = args.req.owner,
        workflow = args.req.workflowId,
        status = args.req.status.toSet,
        parent = args.req.parent,
        clonedFrom = args.req.clonedFrom,
        tags = args.req.tags.toSet,
        q = args.req.q,
        limit = args.req.limit,
        offset = args.req.offset)
      var runs = storage.read(_.runs.list(query))
      runs = runs.copy(results = runs.results.map(run => run.copy(state = run.state.copy(nodes = run.state.nodes.map(_.unsetResult)))))
      ListRunsResponse(runs.results, runs.totalCount)
    }
  }

  override val deleteRun = handle(DeleteRun) { args: DeleteRun.Args =>
    Future {
      storage.write { stores =>
        stores.runs.get(args.req.id) match {
          case None => throw UnknownRunException(args.req.id)
          case Some(run) =>
            if (run.children.nonEmpty) {
              // It is a parent run, cancel and delete child all runs.
              run.children.foreach(scheduler.kill)
              run.children.foreach(stores.runs.delete)
            } else if (run.parent.isDefined) {
              // It is a child run, update or delete parent run.
              stores.runs.get(run.parent.get).foreach { parent =>
                if (parent.children.size > 1) {
                  // There are several child runs left, remove current one from the list.
                  stores.runs.save(parent.copy(children = parent.children.filterNot(_ == run.id)))
                } else {
                  // It was the last child of this run, delete it as it is now useless.
                  stores.runs.delete(parent.id)
                }
              }
            } else {
              // It is a single run, cancel it.
              scheduler.kill(run.id)
            }
            // Finally, delete the run.
            stores.runs.delete(run.id)
        }
        DeleteRunResponse()
      }
    }
  }

  override val killRun = handle(KillRun) { args: KillRun.Args =>
    Future {
      storage.write { stores =>
        val newRun = stores.runs.get(args.req.id) match {
          case None => throw UnknownRunException(args.req.id)
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
  }

  override val updateRun = handle(UpdateRun) { args: UpdateRun.Args =>
    Future {
      storage.write { stores =>
        stores.runs.get(args.req.id) match {
          case None => throw UnknownRunException(args.req.id)
          case Some(run) =>
            var newRun = run.parent.flatMap(stores.runs.get).getOrElse(run)
            args.req.name.foreach(name => newRun = newRun.copy(name = Some(name)))
            args.req.notes.foreach(notes => newRun = newRun.copy(notes = Some(notes)))
            if (args.req.tags.nonEmpty) {
              newRun = newRun.copy(tags = args.req.tags)
            }
            stores.runs.save(newRun)
            UpdateRunResponse(newRun)
        }
      }
    }
  }

  override val listLogs = handle(ListLogs) { args: ListLogs.Args =>
    Future {
      val results = storage.read(_.runs.get(args.req.runId))
        .flatMap(_.state.nodes.find(_.name == args.req.nodeName))
        .flatMap(_.taskId)
        .map(taskId => scheduler.getLogs(taskId, args.req.kind, skip = args.req.skip, tail = args.req.tail))
        .getOrElse(Seq.empty)
      ListLogsResponse(results)
    }
  }
}
