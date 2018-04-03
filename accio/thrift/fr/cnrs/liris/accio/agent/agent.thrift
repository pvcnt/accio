/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-201 8 Vincent Primault <v.primault@ucl.ac.uk>
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

include "accio/thrift/fr/cnrs/liris/accio/api/api.thrift"

struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: required api.OpDef operator;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<api.OpDef> operators;
}

struct PushWorkflowRequest {
  1: required api.Workflow workflow;
}

struct PushWorkflowResponse {
  1: required api.Workflow workflow;
  2: required list<api.FieldViolation> warnings;
}

struct GetWorkflowRequest {
  1: required api.WorkflowId id;
  2: optional string version;
}

struct GetWorkflowResponse {
  1: required api.Workflow workflow;
}

struct ListWorkflowsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional string q;
  4: optional i32 limit;
  5: optional i32 offset;
}

struct ListWorkflowsResponse {
  1: required list<api.Workflow> workflows;
  2: required i32 total_count;
}

struct CreateRunRequest {
  1: required api.Experiment run;
}

struct CreateRunResponse {
  1: required list<api.RunId> ids;
  2: required list<api.FieldViolation> warnings;
}

struct GetRunRequest {
  1: required api.RunId id;
}

struct GetRunResponse {
  1: required api.Run run;
}

struct ListRunsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional api.WorkflowId workflow_id;
  5: required set<api.TaskState> status = [];
  6: required set<string> tags = [];
  7: optional api.RunId parent;
  8: optional api.RunId cloned_from;
  9: optional string q;
  10: optional i32 limit;
  11: optional i32 offset;
}

struct ListRunsResponse {
  1: required list<api.Run> runs;
  2: required i32 total_count;
}

struct DeleteRunRequest {
  1: required api.RunId id;
}

struct DeleteRunResponse {
}

struct KillRunRequest {
  1: required api.RunId id;
}

struct KillRunResponse {
  1: required api.Run run;
}

struct UpdateRunRequest {
  1: required api.RunId id;
  2: optional string name;
  3: optional string notes;
  4: required set<string> tags = [];
}

struct UpdateRunResponse {
  1: required api.Run run;
}

struct ListLogsRequest {
  1: required api.RunId run_id;
  2: required string node_name;
  3: required string kind;
  4: optional i32 tail;
  5: optional i32 skip;
}

struct ListLogsResponse {
  1: required list<string> results;
}

struct ValidateRunRequest {
  1: required api.Experiment run;
}

struct ValidateRunResponse {
  1: required list<api.FieldViolation> warnings;
  2: required list<api.FieldViolation> errors;
}

struct ValidateWorkflowRequest {
  1: required api.Workflow workflow;
}

struct ValidateWorkflowResponse {
  1: required list<api.FieldViolation> warnings;
  2: required list<api.FieldViolation> errors;
}

struct GetClusterRequest {
}

struct GetClusterResponse {
  1: required string cluster_name;
  2: required string version;
}

/**
 * RPC endpoints used by clients to communicate with the agent. It contains the public interface of
 * Accio, exposed to the outside world and available to be consumed by users.
 */
service AgentService {
  // Provide information about this cluster.
  GetClusterResponse getCluster(1: GetClusterRequest req) throws (1: api.ServerException e);

  // Get a specific operator, if it exists.
  GetOperatorResponse getOperator(1: GetOperatorRequest req) throws (1: api.ServerException e);

  // List all known operators.
  ListOperatorsResponse listOperators(1: ListOperatorsRequest req) throws (1: api.ServerException e);

  // Validate that a workflow is valid, and would be accepted by the server.
  ValidateWorkflowResponse validateWorkflow(1: ValidateWorkflowRequest req) throws (1: api.ServerException e);

  // Push a new version of a workflow.
  PushWorkflowResponse pushWorkflow(1: PushWorkflowRequest req) throws (1: api.ServerException e);

  // Get a specific workflow, if it exists.
  GetWorkflowResponse getWorkflow(1: GetWorkflowRequest req) throws (1: api.ServerException e);

  // List all workflows matching some criteria.
  ListWorkflowsResponse listWorkflows(1: ListWorkflowsRequest req) throws (1: api.ServerException e);

  // Validate that a run is valid, and would be accepted by the server.
  ValidateRunResponse validateRun(1: ValidateRunRequest req) throws (1: api.ServerException e);

  // Create a new run (and schedule them).
  CreateRunResponse createRun(1: CreateRunRequest req) throws (1: api.ServerException e);

  // Get a specific run.
  GetRunResponse getRun(1: GetRunRequest req) throws (1: api.ServerException e);

  // Retrieve all runs matching some criteria.
  ListRunsResponse listRuns(1: ListRunsRequest req) throws (1: api.ServerException e);

  // Delete a specific run.
  DeleteRunResponse deleteRun(1: DeleteRunRequest req) throws (1: api.ServerException e);

  // Kill a specific run.
  KillRunResponse killRun(1: KillRunRequest req) throws (1: api.ServerException e)

  // Update some information of a specific run.
  UpdateRunResponse updateRun(1: UpdateRunRequest req) throws (1: api.ServerException e)

  // List log lines.
  ListLogsResponse listLogs(1: ListLogsRequest req) throws (1: api.ServerException e)
}
