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

namespace java fr.cnrs.liris.accio.thrift.agent

include "fr/cnrs/liris/accio/core/application/task_tracker.thrift"

service TaskTrackerService {
  task_tracker.HeartbeatTaskResponse heartbeat(1: task_tracker.HeartbeatTaskRequest req);

  task_tracker.RegisterExecutorResponse register(1: task_tracker.RegisterExecutorRequest req);

  task_tracker.UpdateTaskResponse update(1: task_tracker.UpdateTaskRequest req);

  task_tracker.CompletedTaskResponse completed(1: task_tracker.CompletedTaskRequest req);
}