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

include "fr/cnrs/liris/accio/framework/api/accio.thrift"

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
  1: required accio.Workflow spec;
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
  5: required set<accio.RunStatus> status;
  6: required set<string> tags;
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
  4: required set<string> tags;
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
  1: optional accio.Workflow workflow;
  2: required list<accio.InvalidSpecMessage> warnings;
  3: required list<accio.InvalidSpecMessage> errors;
}

struct GetClusterRequest {
}

struct GetClusterResponse {
  1: required string cluster_name;
  2: required string version;
}

struct ListAgentsRequest {
}

struct ListAgentsResponse {
  1: required list<accio.Agent> results;
}

struct GetDatasetRequest {
  1: required accio.RunId run_id;
  2: required string node_name;
  3: required string port_name;
  4: optional i32 limit;
  5: required bool sample;
}

struct ThriftEvent {
  1: required string user;
  2: required list<i32> location;
  3: required accio.Timestamp timestamp;
}

struct GetDatasetResponse {
  1: required list<ThriftEvent> events;
  2: required i32 total_count;
}