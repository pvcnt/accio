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

namespace java fr.cnrs.liris.accio.core.application.handler

include "fr/cnrs/liris/accio/core/domain/common.thrift"
include "fr/cnrs/liris/accio/core/domain/graph.thrift"
include "fr/cnrs/liris/accio/core/domain/workflow.thrift"
include "fr/cnrs/liris/accio/core/domain/run.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: optional operator.OpDef result;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<operator.OpDef> results;
}

struct PushWorkflowRequest {
  1: required workflow.WorkflowTemplate template;
  2: required common.User user;
}

struct PushWorkflowResponse {
}

struct GetWorkflowRequest {
  1: required common.WorkflowId id;
  2: optional string version;
}

struct GetWorkflowResponse {
  1: optional workflow.Workflow result;
}

struct ListWorkflowsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional i32 limit;
  4: optional i32 offset;
}

struct ListWorkflowsResponse {
  1: required list<workflow.Workflow> results;
  2: required i32 total_count;
}

struct DeleteWorkflowRequest {
  1: required common.WorkflowId id;
}

struct DeleteWorkflowResponse {
}

struct CreateRunRequest {
  1: required run.RunTemplate template;
  2: required common.User user;
}

struct CreateRunResponse {
  1: required list<common.RunId> ids;
}

struct GetRunRequest {
  1: required common.RunId id;
}

struct GetRunResponse {
  1: optional run.Run result;
}

struct ListRunsRequest {
  1: optional string owner;
  2: optional string name;
  3: optional string cluster;
  4: optional string environment;
  5: optional run.RunStatus status;
  6: optional set<string> tags;
  7: optional common.RunId parent;
  8: optional common.RunId cloned_from;
  9: optional common.WorkflowId workflow_id;
  10: optional i32 limit;
  11: optional i32 offset;
}

struct ListRunsResponse {
  1: required list<run.Run> results;
  2: required i32 total_count;
}

struct DeleteRunRequest {
  1: required common.RunId id;
}

struct DeleteRunResponse {
}

struct KillRunRequest {
  1: required common.RunId id;
}

struct KillRunResponse {
}