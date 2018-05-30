/*
 * Accio is a platform to launch computer science experiments.
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

namespace java fr.cnrs.liris.lumos.domain.thrift

struct RemoteFile {
  1: string uri;
  2: optional string content_type;
  3: optional string format;
  4: optional string sha256;
}

union ValuePayload {
  1: i32 int;
  2: i64 long;
  3: double float;
  4: double dbl;
  5: string str;
  6: bool boolean;
  7: RemoteFile file;
}

struct Value {
  1: string data_type;
  2: ValuePayload payload;
}

struct MetricValue {
  1: string name;
  2: double value;
  3: set<string> aspects;
}

struct AttrValue {
  1: string name;
  2: Value value;
  3: set<string> aspects;
}

enum ExecState {
  PENDING,
  SCHEDULED,
  RUNNING,
  SUCCESSFUL,
  FAILED,
  CANCELED,
  LOST,
}

struct ExecStatus {
  1: ExecState state;
  2: i64 time;
  3: optional string message;
}

struct ErrorDatum {
  1: string mnemonic;
  2: optional string message;
  3: list<string> stacktrace;
}

struct Task {
  1: string name;
  2: optional string mnemonic;
  3: set<string> dependencies;
  4: map<string, string> metadata;
  5: optional i32 exit_code;
  6: list<MetricValue> metrics;
  7: optional ErrorDatum error;
  8: optional ExecStatus status;
  9: list<ExecStatus> history;
}

struct Job {
  1: string name;
  2: i64 create_time = 0;
  3: optional string owner;
  4: optional string contact;
  5: map<string, string> labels;
  6: map<string, string> metadata;
  7: list<AttrValue> inputs;
  8: list<AttrValue> outputs;
  9: i32 progress = 0;
  10: list<Task> tasks;
  11: optional ExecStatus status;
  12: list<ExecStatus> history;
}

struct JobEnqueuedEvent {
  1: Job job;
}

struct JobExpandedEvent {
  1: list<Task> tasks;
}

struct JobScheduledEvent {
  1: map<string, string> metadata;
  2: optional string message;
}

struct JobStartedEvent {
  1: optional string message;
}

struct JobCompletedEvent {
  1: list<AttrValue> outputs;
  2: optional string message;
}

struct JobCanceledEvent {
  1: optional string message;
}

struct TaskScheduledEvent {
  1: string name;
  2: map<string, string> metadata;
  3: optional string message;
}

struct TaskStartedEvent {
  1: string name;
  2: optional string message;
}

struct TaskCompletedEvent {
  1: string name;
  2: i32 exit_code;
  3: list<MetricValue> metrics;
  4: optional ErrorDatum error;
  5: optional string message;
}

union EventPayload {
  1: JobEnqueuedEvent job_enqueued;
  2: JobExpandedEvent job_expanded;
  3: JobScheduledEvent job_scheduled;
  4: JobStartedEvent job_started;
  5: JobCompletedEvent job_completed;
  6: JobCanceledEvent job_canceled;
  7: TaskScheduledEvent task_scheduled;
  8: TaskStartedEvent task_started;
  9: TaskCompletedEvent task_completed;
}

struct Event {
  1: string parent;
  2: i64 sequence;
  3: i64 time;
  4: EventPayload payload;
}