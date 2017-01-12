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

typedef i64 Timestamp

struct WorkflowId {
  1: string value;
}

struct RunId {
  1: string value;
}

struct TaskId {
  1: string value;
}

enum AtomicType {
  BYTE,
  INTEGER,
  LONG,
  DOUBLE,
  STRING,
  BOOLEAN,
  LOCATION,
  TIMESTAMP,
  DURATION,
  DISTANCE,
  DATASET,
  LIST,
  SET,
  MAP,
}

struct DataType {
  1: required AtomicType base;
  2: required list<AtomicType> args;
}

struct Value {
  1: required i32 size = 1;
  2: list<string> strings;
  3: list<i64> longs;
  4: list<double> doubles;
  5: list<i32> integers;
  6: list<bool> booleans;
  7: list<byte> bytes;
}

struct Artifact {
  // Artifact name.
  1: required string name;

  // Data type.
  2: required DataType kind;

  // Value, that should be consistent with above data type.
  3: required Value value;
}

struct Metric {
  // Metric name.
  1: required string name;

  // Value.
  2: required double value;
}

struct ErrorData {
  1: required string classifier;
  2: optional string message;
  3: required list<string> stacktrace;
}

struct Error {
  1: required ErrorData root;
  2: required list<ErrorData> causes;
}

struct User {
  // User name.
  1: required string name;

  // Email address.
  2: optional string email;
}

struct Reference {
  1: required string node;
  2: required string port;
}