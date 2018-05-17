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

namespace java fr.cnrs.liris.accio.server

include "accio/thrift/fr/cnrs/liris/accio/domain/accio.thrift"
include "accio/thrift/fr/cnrs/liris/infra/thriftserver/errors.thrift"

struct GetInfoResponse {
  1: required string version;
}

struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: required accio.Operator operator;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<accio.Operator> operators;
}

struct SubmitWorkflowRequest {
  1: required accio.Workflow workflow;
}

struct SubmitWorkflowResponse {
  1: required string name;
  2: required list<string> job_names;
  3: required list<errors.FieldViolation> warnings;
}

struct KillWorkflowRequest {
  1: required string name;
}

struct KillWorkflowResponse {
}

struct ValidateWorkflowRequest {
  1: required accio.Workflow workflow;
}

struct ValidateWorkflowResponse {
  1: required list<errors.FieldViolation> warnings;
  2: required list<errors.FieldViolation> errors;
}

service AccioService {
  // Get information about this server.
  GetInfoResponse getInfo() throws (1: errors.ServerError e);

  // Get a specific operator, if it exists.
  GetOperatorResponse getOperator(1: GetOperatorRequest req) throws (1: errors.ServerError e);

  // List all known operators.
  ListOperatorsResponse listOperators(1: ListOperatorsRequest req) throws (1: errors.ServerError e);

  // Validate that a job is valid, and would be accepted by the server.
  ValidateWorkflowResponse validateWorkflow(1: ValidateWorkflowRequest req) throws (1: errors.ServerError e);

  // Submit a new workflow.
  SubmitWorkflowResponse submitWorkflow(1: SubmitWorkflowRequest req) throws (1: errors.ServerError e);

  // Kill a specific workflow.
  KillWorkflowResponse killWorkflow(1: KillWorkflowRequest req) throws (1: errors.ServerError e);
}
