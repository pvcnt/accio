/*
 * Accio is a program whose purpose is to study location privacy.
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

include "fr/cnrs/liris/accio/framework/api/accio.thrift"

struct RegisterWorkerRequest {
  // Identifier of this worker, which should be unique across all workers.
  1: required accio.WorkerId worker_id;

  // Finagle name at which the worker's RPC endpoint lives.
  2: required string dest;

  // Maximum available resources that can be used on this worker.
  3: required accio.Resource max_resources;
}

struct RegisterWorkerResponse {
}

struct UnregisterWorkerRequest {
  1: required accio.WorkerId worker_id;
}

struct UnregisterWorkerResponse {
}

struct HeartbeatWorkerRequest {
  1: required accio.WorkerId worker_id;
}

struct HeartbeatWorkerResponse {
}

/**
 * Communication protocol between master and executors.
 */
struct StartTaskRequest {
  1: required accio.WorkerId worker_id;
  2: required accio.TaskId task_id;
}

struct StartTaskResponse {
  1: required accio.RunId run_id;
  2: required string node_name;
  3: required accio.OpPayload payload;
}

struct StreamTaskLogsRequest {
  1: required accio.WorkerId worker_id;
  2: required accio.TaskId task_id;
  3: required list<accio.RunLog> logs;
}

struct StreamTaskLogsResponse {
}

struct CompleteTaskRequest {
  1: required accio.WorkerId worker_id;
  2: required accio.TaskId task_id;
  3: required accio.OpResult result;
}

struct CompleteTaskResponse {
}

struct LostTaskRequest {
  1: required accio.WorkerId worker_id;
  2: required accio.TaskId task_id;
}

struct LostTaskResponse {
}