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
include "accio/thrift/fr/cnrs/liris/accio/api/errors.thrift"

struct GetOperatorRequest {
  1: required string name;
}

struct GetOperatorResponse {
  1: required api.Operator operator;
}

struct ListOperatorsRequest {
  1: required bool include_deprecated = false;
}

struct ListOperatorsResponse {
  1: required list<api.Operator> operators;
}

struct CreateJobRequest {
  1: required api.Job job;
}

struct CreateJobResponse {
  1: required api.Job job;
  2: required list<errors.FieldViolation> warnings;
}

struct GetJobRequest {
  1: required string name;
}

struct GetJobResponse {
  1: required api.Job job;
}

struct ListJobsRequest {
  1: optional string author;
  2: optional string title;
  5: optional set<api.ExecState> state;
  6: optional set<string> tags;
  7: optional string parent;
  8: optional string cloned_from;
  9: optional string q;
  10: optional i32 limit;
  11: optional i32 offset;
}

struct ListJobsResponse {
  1: required list<api.Job> jobs;
  2: required i32 total_count;
}

struct DeleteJobRequest {
  1: required string name;
}

struct DeleteJobResponse {
}

struct KillJobRequest {
  1: required string name;
}

struct KillJobResponse {
}

struct ListLogsRequest {
  1: required string job;
  2: required string step;
  3: required string kind;
  4: optional i32 tail;
  5: optional i32 skip;
}

struct ListLogsResponse {
  1: required list<string> results;
}

struct ValidateJobRequest {
  1: required api.Job job;
}

struct ValidateJobResponse {
  1: required list<errors.FieldViolation> warnings;
  2: required list<errors.FieldViolation> errors;
}

struct GetClusterRequest {
}

struct GetClusterResponse {
  1: required string version;
}

/**
 * RPC endpoints used by clients to communicate with the agent. It contains the public interface of
 * Accio, exposed to the outside world and available to be consumed by users.
 */
service AgentService {
  // Provide information about this cluster.
  GetClusterResponse getCluster(1: GetClusterRequest req) throws (1: errors.ServerException e);

  // Get a specific operator, if it exists.
  GetOperatorResponse getOperator(1: GetOperatorRequest req) throws (1: errors.ServerException e);

  // List all known operators.
  ListOperatorsResponse listOperators(1: ListOperatorsRequest req) throws (1: errors.ServerException e);

  // Validate that a job is valid, and would be accepted by the server.
  ValidateJobResponse validateJob(1: ValidateJobRequest req) throws (1: errors.ServerException e);

  // Create a new job (and schedule it).
  CreateJobResponse createJob(1: CreateJobRequest req) throws (1: errors.ServerException e);

  // Get a specific job.
  GetJobResponse getJob(1: GetJobRequest req) throws (1: errors.ServerException e);

  // Retrieve all jobs matching some criteria.
  ListJobsResponse listJobs(1: ListJobsRequest req) throws (1: errors.ServerException e);

  // Delete a specific job.
  DeleteJobResponse deleteJob(1: DeleteJobRequest req) throws (1: errors.ServerException e);

  // Kill a specific job.
  KillJobResponse killJob(1: KillJobRequest req) throws (1: errors.ServerException e);

  // List log lines.
  ListLogsResponse listLogs(1: ListLogsRequest req) throws (1: errors.ServerException e);
}
