/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

include "fr/cnrs/liris/accio/core/application/agent.thrift"

service AgentService {
  agent.GetOperatorResponse getOperator(1: agent.GetOperatorRequest req);

  agent.ListOperatorsResponse listOperators(1: agent.ListOperatorsRequest req);

  agent.PushWorkflowResponse pushWorkflow(1: agent.PushWorkflowRequest req);

  agent.GetWorkflowResponse getWorkflow(1: agent.GetWorkflowRequest req);

  agent.ListWorkflowsResponse listWorkflows(1: agent.ListWorkflowsRequest req);

  agent.CreateRunResponse createRun(1: agent.CreateRunRequest req);

  agent.GetRunResponse getRun(1: agent.GetRunRequest req);

  agent.ListRunsResponse listRuns(1: agent.ListRunsRequest req);

  agent.DeleteRunResponse deleteRun(1: agent.DeleteRunRequest req);

  agent.KillRunResponse killRun(1: agent.KillRunRequest req);

  agent.HeartbeatResponse heartbeat(1: agent.HeartbeatRequest req);

  agent.StartTaskResponse startTask(1: agent.StartTaskRequest req);

  agent.StreamLogsResponse streamLogs(1: agent.StreamLogsRequest req);

  agent.CompleteTaskResponse completeTask(1: agent.CompleteTaskRequest req);
}