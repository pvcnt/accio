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

namespace java fr.cnrs.liris.accio.core.domain

include "fr/cnrs/liris/accio/core/domain/common.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

enum TaskStatus {
  SCHEDULED,
  RUNNING,
  SUCCESS,
  FAILED,
  KILLED,
  LOST,
}

struct TaskState {
  1: required TaskStatus status;
  2: optional common.Timestamp started_at;
  3: optional common.Timestamp completed_at;
  4: optional common.Timestamp heartbeat_at;
  5: optional double progress;
}

struct Task {
  1: required common.TaskId id;
  2: required common.RunId run_id;
  3: required string node_name;
  4: required operator.OpPayload payload;
  5: required string key;
  6: required string scheduler;
  7: required common.Timestamp created_at;
  8: required TaskState state;
}

struct TaskLog {
  1: required common.TaskId task_id;
  2: required string classifier;
  3: required common.Timestamp created_at;
  4: required string message;
}

