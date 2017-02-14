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

/**
 * Communication protocol between clients and servers.
 */
struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: optional accio.OpDef result;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<accio.OpDef> results;
}

struct PushWorkflowRequest {
  1: required accio.WorkflowSpec spec;
  2: required accio.User user;
}

struct PushWorkflowResponse {
  1: required accio.Workflow workflow;
  2: required list<accio.InvalidSpecMessage> warnings;
}

struct GetWorkflowRequest {
  1: required accio.WorkflowId id;
  2: optional string version;
}

struct GetWorkflowResponse {
  1: optional accio.Workflow result;
}

struct ListWorkflowsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional string q;
  4: optional i32 limit;
  5: optional i32 offset;
}

struct ListWorkflowsResponse {
  1: required list<accio.Workflow> results;
  2: required i32 total_count;
}

struct CreateRunRequest {
  1: required accio.RunSpec spec;
  2: required accio.User user;
}

struct CreateRunResponse {
  1: required list<accio.RunId> ids;
  2: required list<accio.InvalidSpecMessage> warnings;
}

struct GetRunRequest {
  1: required accio.RunId id;
}

struct GetRunResponse {
  1: optional accio.Run result;
}

struct ListRunsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional accio.WorkflowId workflow_id;
  5: optional set<accio.RunStatus> status;
  6: optional set<string> tags;
  7: optional accio.RunId parent;
  8: optional accio.RunId cloned_from;
  9: optional string q;
  10: optional i32 limit;
  11: optional i32 offset;
}

struct ListRunsResponse {
  1: required list<accio.Run> results;
  2: required i32 total_count;
}

struct DeleteRunRequest {
  1: required accio.RunId id;
}

struct DeleteRunResponse {
}

struct KillRunRequest {
  1: required accio.RunId id;
}

struct KillRunResponse {
  1: required accio.Run run;
}

struct UpdateRunRequest {
  1: required accio.RunId id;
  2: optional string name;
  3: optional string notes;
  4: optional set<string> tags;
}

struct UpdateRunResponse {
}

struct ListLogsRequest {
  1: required accio.RunId run_id;
  2: required string node_name;
  3: optional string classifier;
  4: optional i32 limit;
  5: optional accio.Timestamp since;
}

struct ListLogsResponse {
  1: required list<accio.RunLog> results;
}

struct ParseRunRequest {
  1: required string content;
  2: required map<string, string> params;
  3: optional string filename;
}

struct ParseRunResponse {
  1: optional accio.RunSpec run;
  2: required list<accio.InvalidSpecMessage> warnings;
  3: required list<accio.InvalidSpecMessage> errors;
}

struct ParseWorkflowRequest {
  1: required string content;
  2: optional string filename;
}

struct ParseWorkflowResponse {
  1: optional accio.WorkflowSpec workflow;
  2: required list<accio.InvalidSpecMessage> warnings;
  3: required list<accio.InvalidSpecMessage> errors;
}

struct InfoRequest {
}

struct InfoResponse {
  1: required string cluster_name;
  2: required string version;
}

struct UpdateRequest {
}

struct UpdateResponse {
}

exception InvalidTaskException {
}

exception UnknownRunException {
}

/**
 * Communication protocol between servers and workers.
 **/
struct ScheduleTaskRequest {
  1: required accio.Task task;
}

struct ScheduleTaskResponse {
  1: required bool accepted;
}

struct KillTaskRequest {
  1: required string key;
}

struct KillTaskResponse {
  1: required bool accepted;
}

/**
 * Communication protocol between servers and executors.
 */
struct StartTaskRequest {
  1: required accio.TaskId task_id;
}

struct StartTaskResponse {
  1: required accio.RunId run_id;
  2: required string node_name;
  3: required accio.OpPayload payload;
}

struct HeartbeatRequest {
  1: required accio.TaskId task_id;
}

struct HeartbeatResponse {
}

struct StreamLogsRequest {
  1: required list<accio.RunLog> logs;
}

struct StreamLogsResponse {
}

struct CompleteTaskRequest {
  1: required accio.TaskId task_id;
  2: required accio.OpResult result;
}

struct CompleteTaskResponse {
}

service AgentService {
  /**
   * Communication protocol with clients.
   */
  // Retrieve a specific operator, if it exists.
  GetOperatorResponse getOperator(1: GetOperatorRequest req);

  // Retrieve all known operators.
  ListOperatorsResponse listOperators(1: ListOperatorsRequest req);

  // Parse a string, written using the workflow DSL, into a valid accio.
  ParseWorkflowResponse parseWorkflow(1: ParseWorkflowRequest req);

  // Push a new version of a accio.
  PushWorkflowResponse pushWorkflow(1: PushWorkflowRequest req) throws (1: accio.InvalidSpecException parse);

  GetWorkflowResponse getWorkflow(1: GetWorkflowRequest req);

  ListWorkflowsResponse listWorkflows(1: ListWorkflowsRequest req);

  // Parse a string, written using the run DSL, into a valid run specification.
  ParseRunResponse parseRun(1: ParseRunRequest req);

  CreateRunResponse createRun(1: CreateRunRequest req) throws (1: accio.InvalidSpecException parse);

  GetRunResponse getRun(1: GetRunRequest req);

  ListRunsResponse listRuns(1: ListRunsRequest req);

  DeleteRunResponse deleteRun(1: DeleteRunRequest req) throws (1: UnknownRunException unknown);

  KillRunResponse killRun(1: KillRunRequest req) throws (1: UnknownRunException unknown);

  UpdateRunResponse updateRun(1: UpdateRunRequest req) throws (1: UnknownRunException unknown);

  ListLogsResponse listLogs(1: ListLogsRequest req);

  UpdateResponse update(1: UpdateRequest req);

  InfoResponse info(1: InfoRequest req);

  /**
   * Communication protocol with executors.
   */
  HeartbeatResponse heartbeat(1: HeartbeatRequest req) throws (1: InvalidTaskException invalid);

  StartTaskResponse startTask(1: StartTaskRequest req) throws (1: InvalidTaskException invalid);

  StreamLogsResponse streamLogs(1: StreamLogsRequest req) throws (1: InvalidTaskException invalid);

  CompleteTaskResponse completeTask(1: CompleteTaskRequest req) throws (1: InvalidTaskException invalid);
}

service WorkerService {

}