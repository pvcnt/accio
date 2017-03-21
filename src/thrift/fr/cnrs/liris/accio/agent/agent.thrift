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

namespace java fr.cnrs.liris.accio.agent

include "fr/cnrs/liris/accio/core/domain/accio.thrift"
include "fr/cnrs/liris/accio/agent/api.thrift"
include "fr/cnrs/liris/accio/agent/worker.thrift"
include "fr/cnrs/liris/accio/agent/master.thrift"

service AgentService {
  /**
   * RPC endpoints used by clients to communicate with masters. It contains the public interface of Accio, exposed
   * to the outside world and available to be consumed by users.
   */
  // Provide information about this cluster.
  api.GetClusterResponse getCluster(1: api.GetClusterRequest req);

  // List all agents inside this cluster.
  api.ListAgentsResponse listAgents(1: api.ListAgentsRequest req);

  // Get a specific operator, if it exists.
  api.GetOperatorResponse getOperator(1: api.GetOperatorRequest req);

  // List all known operators.
  api.ListOperatorsResponse listOperators(1: api.ListOperatorsRequest req);

  // Parse a string, written using the workflow DSL, into a (hopefully) valid workflow specification.
  api.ParseWorkflowResponse parseWorkflow(1: api.ParseWorkflowRequest req);

  // Push a new version of a workflow.
  api.PushWorkflowResponse pushWorkflow(1: api.PushWorkflowRequest req)
    throws (1: accio.InvalidSpecException parse);

  // Get a specific workflow, if it exists.
  api.GetWorkflowResponse getWorkflow(1: api.GetWorkflowRequest req);

  // List all workflows matching some criteria.
  api.ListWorkflowsResponse listWorkflows(1: api.ListWorkflowsRequest req);

  // Parse a string, written using the run DSL, into a (hopefully) valid run specification.
  api.ParseRunResponse parseRun(1: api.ParseRunRequest req);

  // Create a new run (and schedule them).
  api.CreateRunResponse createRun(1: api.CreateRunRequest req)
    throws (1: accio.InvalidSpecException parse);

  // Get a specific run.
  api.GetRunResponse getRun(1: api.GetRunRequest req);

  // Retrieve all runs matching some criteria.
  api.ListRunsResponse listRuns(1: api.ListRunsRequest req);

  // Delete a specific run.
  api.DeleteRunResponse deleteRun(1: api.DeleteRunRequest req)
    throws (1: accio.UnknownRunException unknown);

  // Kill a specific run.
  api.KillRunResponse killRun(1: api.KillRunRequest req)
    throws (1: accio.UnknownRunException unknown);

  // Update some information of a specific run.
  api.UpdateRunResponse updateRun(1: api.UpdateRunRequest req)
    throws (1: accio.UnknownRunException unknown);

  // List log lines.
  api.ListLogsResponse listLogs(1: api.ListLogsRequest req);

  /**
   * RPC endpoints used by workers to communicate with their master.
   */
  // Register a new worker. This is mandatory to allow a worker to use any other endpoint.
  master.RegisterWorkerResponse registerWorker(1: master.RegisterWorkerRequest req);

  // Unregister a worker. It will then not be able to use any other endpoint.
  master.UnregisterWorkerResponse unregisterWorker(1: master.UnregisterWorkerRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker);

  // Heartbeat coming from a worker, signaling it is alive.
  master.HeartbeatWorkerResponse heartbeatWorker(1: master.HeartbeatWorkerRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker);

  // Indicates that an executor is ready to start processing a task, through its worker.
  master.StartTaskResponse startTask(1: master.StartTaskRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker, 2: accio.InvalidTaskException invalidTask);

  // Stream log lines from an executor, through its worker.
  master.StreamTaskLogsResponse streamTaskLogs(1: master.StreamTaskLogsRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker, 2: accio.InvalidTaskException invalidTask);

  // Indicates that an executor completed processing a task, through its worker.
  master.CompleteTaskResponse completeTask(1: master.CompleteTaskRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker, 2: accio.InvalidTaskException invalidTask);

  // Indicates that a worker lost contact with an executor.
  master.LostTaskResponse lostTask(1: master.LostTaskRequest req)
    throws (1: accio.InvalidWorkerException invalidWorker, 2: accio.InvalidTaskException invalidTask);

  /**
   * RPC endpoints used by master to communicate with their workers.
   */
  worker.AssignTaskResponse assignTask(1: worker.AssignTaskRequest req)
    throws (1: accio.InvalidTaskException invalidTask);

  worker.KillTaskResponse killTask(1: worker.KillTaskRequest req)
    throws (1: accio.InvalidTaskException invalidTask);

  /**
   * RPC endpoints used by executors to communicate with their worker.
   */
  worker.HeartbeatExecutorResponse heartbeatExecutor(1: worker.HeartbeatExecutorRequest req)
    throws (1: accio.InvalidExecutorException invalidExecutor);

  worker.StartExecutorResponse startExecutor(1: worker.StartExecutorRequest req)
    throws (1: accio.InvalidExecutorException invalidExecutor, 2: accio.InvalidTaskException invalidTask);

  worker.StreamExecutorLogsResponse streamExecutorLogs(1: worker.StreamExecutorLogsRequest req)
    throws (1: accio.InvalidExecutorException invalidExecutor, 2: accio.InvalidTaskException invalidTask, 3: accio.InvalidWorkerException invalidWorker);

  worker.StopExecutorResponse stopExecutor(1: worker.StopExecutorRequest req)
    throws (1: accio.InvalidExecutorException invalidExecutor, 2: accio.InvalidTaskException invalidTask);
}