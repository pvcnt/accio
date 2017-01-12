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
include "fr/cnrs/liris/accio/core/domain/run.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

struct RegisterExecutorRequest {
  1: required common.TaskId task_id;
}

struct RegisterExecutorResponse {
  1: required common.RunId run_id;
  2: required string node_name;
  3: required operator.OpPayload payload;
}

struct HeartbeatTaskRequest {
  1: required common.TaskId task_id;
}

struct HeartbeatTaskResponse {
}

struct StreamLogsRequest {
  1: required list<run.RunLog> logs;
}

struct StreamLogsResponse {
}

struct CompletedTaskRequest {
  1: required common.TaskId task_id;
  2: required operator.OpResult result;
}

struct CompletedTaskResponse {
}