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

namespace java fr.cnrs.liris.accio.agent

include "fr/cnrs/liris/accio/api/api.thrift"

struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: optional api.OpDef result;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<api.OpDef> results;
}

struct PushWorkflowRequest {
  1: required api.Workflow spec;
  2: required api.User user;
}

struct PushWorkflowResponse {
  1: required api.Workflow workflow;
  2: required list<api.InvalidSpecMessage> warnings;
}

struct GetWorkflowRequest {
  1: required api.WorkflowId id;
  2: optional string version;
}

struct GetWorkflowResponse {
  1: optional api.Workflow result;
}

struct ListWorkflowsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional string q;
  4: optional i32 limit;
  5: optional i32 offset;
}

struct ListWorkflowsResponse {
  1: required list<api.Workflow> results;
  2: required i32 total_count;
}

struct CreateRunRequest {
  1: required api.RunSpec spec;
  2: required api.User user;
}

struct CreateRunResponse {
  1: required list<api.RunId> ids;
  2: required list<api.InvalidSpecMessage> warnings;
}

struct GetRunRequest {
  1: required api.RunId id;
}

struct GetRunResponse {
  1: optional api.Run result;
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
  1: required list<api.Run> results;
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
}

struct ListLogsRequest {
  1: required api.RunId run_id;
  2: required string node_name;
  3: optional string classifier;
  4: optional i32 limit;
  5: optional api.Timestamp since;
}

struct ListLogsResponse {
  1: required list<api.RunLog> results;
}

struct ParseRunRequest {
  1: required string content;
  2: required map<string, string> params;
  3: optional string filename;
}

struct ParseRunResponse {
  1: optional api.RunSpec run;
  2: required list<api.InvalidSpecMessage> warnings;
  3: required list<api.InvalidSpecMessage> errors;
}

struct ParseWorkflowRequest {
  1: required string content;
  2: optional string filename;
}

struct ParseWorkflowResponse {
  1: optional api.Workflow workflow;
  2: required list<api.InvalidSpecMessage> warnings;
  3: required list<api.InvalidSpecMessage> errors;
}

struct GetClusterRequest {
}

struct GetClusterResponse {
  1: required string cluster_name;
  2: required string version;
}

service AgentService {
  /**
   * RPC endpoints used by clients to communicate with masters. It contains the public interface of Accio, exposed
   * to the outside world and available to be consumed by users.
   */
  // Provide information about this cluster.
  GetClusterResponse getCluster(1: GetClusterRequest req);

  // Get a specific operator, if it exists.
  GetOperatorResponse getOperator(1: GetOperatorRequest req);

  // List all known operators.
  ListOperatorsResponse listOperators(1: ListOperatorsRequest req);

  // Parse a string, written using the workflow DSL, into a (hopefully) valid workflow specification.
  ParseWorkflowResponse parseWorkflow(1: ParseWorkflowRequest req);

  // Push a new version of a workflow.
  PushWorkflowResponse pushWorkflow(1: PushWorkflowRequest req)
    throws (1: api.InvalidSpecException parse);

  // Get a specific workflow, if it exists.
  GetWorkflowResponse getWorkflow(1: GetWorkflowRequest req);

  // List all workflows matching some criteria.
  ListWorkflowsResponse listWorkflows(1: ListWorkflowsRequest req);

  // Parse a string, written using the run DSL, into a (hopefully) valid run specification.
  ParseRunResponse parseRun(1: ParseRunRequest req);

  // Create a new run (and schedule them).
  CreateRunResponse createRun(1: CreateRunRequest req)
    throws (1: api.InvalidSpecException parse);

  // Get a specific run.
  GetRunResponse getRun(1: GetRunRequest req);

  // Retrieve all runs matching some criteria.
  ListRunsResponse listRuns(1: ListRunsRequest req);

  // Delete a specific run.
  DeleteRunResponse deleteRun(1: DeleteRunRequest req)
    throws (1: api.UnknownRunException unknown);

  // Kill a specific run.
  KillRunResponse killRun(1: KillRunRequest req)
    throws (1: api.UnknownRunException unknown);

  // Update some information of a specific run.
  UpdateRunResponse updateRun(1: UpdateRunRequest req)
    throws (1: api.UnknownRunException unknown);

  // List log lines.
  ListLogsResponse listLogs(1: ListLogsRequest req);
}