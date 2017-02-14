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

service WorkerService {
  ScheduleTaskResponse scheduleTask(1: ScheduleTaskRequest req);

  KillTaskResponse killTask(1: KillTaskRequest req);
}