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

/**
 * Declaration of resources an operator needs to be executed.
 */
struct Resource {
  // Fractional number of CPUs.
  1: required double cpu;

  // Quantity of free memory, in MB.
  2: required i64 ram_mb;

  // Quantity of free disk space, in MB.
  3: required i64 disk_mb;
}

/**
 * Definition of an operator port (either input or output).
 */
struct ArgDef {
  // Input name. Should be unique among all inputs of a given operator.
  1: required string name;

  // One-line help text.
  2: optional string help;

  // Data type.
  3: required common.DataType kind;

  // Whether this parameter is optional and does not have to be specified.
  // Always false for outputs.
  4: required bool is_optional;

  // Default value taken by this input if none is specified.
  // Always empty for outputs.
  5: optional common.Value default_value;
}

/**
 * Definition of an operator.
 */
struct OpDef {
  // Operator name. Should be unique among all operators.
  1: required string name;

  // Category. Only used for presentational purposes.
  2: required string category;

  // One-line help text.
  3: optional string help;

  // Longer description of what the operator does.
  4: optional string description;

  // Definition of inputs the operator consumes.
  5: required list<ArgDef> inputs;

  // Definition of outputs the operator produces.
  6: required list<ArgDef> outputs;

  // Deprecation message, if this operator is actually deprecated.
  7: optional string deprecation;

  // Declaration of resources this operator needs to be executed.
  8: required Resource resource;
}

struct OpPayload {
  1: required string op;
  2: required i64 seed;
  3: required map<string, common.Value> inputs;
}

struct OpResult {
  1: required i32 exit_code;
  2: optional common.Error error;
  3: required set<common.Artifact> artifacts;
  4: required set<common.Metric> metrics;
}