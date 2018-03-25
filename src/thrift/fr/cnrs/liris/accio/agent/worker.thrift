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

include "fr/cnrs/liris/accio/api/accio.thrift"

struct AssignTaskRequest {
  1: required accio.Task task;
}

struct AssignTaskResponse {
}

struct KillTaskRequest {
  1: required accio.TaskId id;
}

struct KillTaskResponse {
}

struct StartExecutorRequest {
  1: required accio.ExecutorId executor_id;
  2: required accio.TaskId task_id;
}

struct StartExecutorResponse {
  1: required accio.RunId run_id;
  2: required string node_name;
  3: required accio.OpPayload payload;
}

struct HeartbeatExecutorRequest {
  1: required accio.ExecutorId executor_id;
}

struct HeartbeatExecutorResponse {
}

struct StreamExecutorLogsRequest {
  1: required accio.ExecutorId executor_id;
  2: required accio.TaskId task_id;
  3: required list<accio.RunLog> logs;
}

struct StreamExecutorLogsResponse {
}

struct StopExecutorRequest {
  1: required accio.ExecutorId executor_id;
  2: required accio.TaskId task_id;
  3: required accio.OpResult result;
}

struct StopExecutorResponse {
}