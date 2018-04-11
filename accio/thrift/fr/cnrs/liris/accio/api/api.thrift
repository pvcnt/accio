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

namespace java fr.cnrs.liris.accio.api.thrift

typedef i64 Timestamp

enum AtomicType {
  INTEGER,
  LONG,
  FLOAT,
  DOUBLE,
  STRING,
  BOOLEAN,
  LOCATION,
  TIMESTAMP,
  DURATION,
  DISTANCE,
}

struct DatasetType {
  1: map<string, AtomicType> schema;
}

struct MapType {
  1: AtomicType keys;
  2: AtomicType values;
}

struct ListType {
  1: AtomicType values;
}

union DataType {
  1: AtomicType atomic;
  2: MapType map_type;
  3: ListType list_type;
  4: DatasetType dataset;
}

struct Value {
  1: DataType data_type;
  2: list<string> strings;
  3: list<i64> longs;
  4: list<double> doubles;
  5: list<i32> integers;
  6: list<bool> booleans;
  7: list<byte> bytes;
}

struct NamedValue {
  1: string name;
  2: Value value;
}

struct Metric {
  1: string name;

  // Value.
  2: double value;

  // Unit in which the metric is expressed.
  3: optional string unit;
}

struct User {
  // User name.
  1: string name;

  // Email address.
  2: optional string email;

  3: set<string> groups;
}

/**
 * Declaration of resources an operator required to execute.
 */
struct ComputeResources {
  // Number of CPUs.
  1: i32 cpus = 0;

  // Quantity of free memory, in MB.
  2: i64 ram_mb = 0;

  // Quantity of free disk space, in GB.
  3: i64 disk_gb = 0;
}

/**
 * Definition of an operator port (either input or output).
 */
struct Attribute {
  1: string name;

  // Data type.
  2: DataType data_type;

  // One-line help text.
  3: optional string help;

  // Default value taken by this input if none is specified. It should be empty for output ports.
  4: optional Value default_value;

  5: bool is_optional = false;
}

/**
 * Definition of an operator.
 */
struct Operator {
  // Operator name. Should be unique among all operators.
  1: string name;

  // Category. Only used for presentational purposes.
  2: string category;

  // One-line help text.
  3: optional string help;

  // Longer description of what the operator does.
  4: optional string description;

  // Definition of inputs the operator consumes.
  5: list<Attribute> inputs;

  // Definition of outputs the operator produces.
  6: list<Attribute> outputs;

  // Deprecation message, if this operator is actually deprecated.
  7: optional string deprecation;

  // Declaration of resources this operator needs to be executed.
  8: ComputeResources resource;

  // Whether this operator is unstable, will produce deterministic outputs given the some inputs. Unstable operators
  // need a random seed specified to produce deterministic outputs.
  9: bool unstable;

  // Path to the executable implementing this operator.
  10: string executable;
}

/**
 * Status of the execution of a task.
 **/
enum ExecState {
  // Waiting for all dependencies to be satisfied.
  PENDING,
  // Submitted to the scheduler.
  SCHEDULED,
  // A task executing this node is running.
  RUNNING,
  // Completed, successfully.
  SUCCESSFUL,
  // Completed, but resulted in a failure.
  FAILED,
  // Killed by the client.
  KILLED,
  // Cancelled, because a dependency resulted in a failure.
  CANCELLED,
}

/**
 * State of a node, as part of a run. This structure is regularly updated as the execution of a node progresses.
 */
struct Task {
  1: string name;

  // Node execution status.
  2: ExecState state = ExecState.PENDING;

  // Time at which the execution of the node started.
  3: optional Timestamp start_time;

  // Time at which the execution of the node completed.
  4: optional Timestamp end_time;

  // Exit code. It is particularly useful for operators launching external executables and monitoring their execution.
  // For built-in operators, it should still be included. 0 means a successful execution, any other value represents
  // a failed execution.
  5: optional i32 exit_code;

  // Metrics produced by the operator execution. This can always be filled, whether or not the execution is successful.
  // There is no definition of metrics produced by an operator, so any relevant metrics can be included here and will
  // be exposed thereafter.
  6: optional list<Metric> metrics;

  7: optional list<NamedValue> artifacts;
}

struct JobStatus {
  1: optional Timestamp start_time;
  2: optional Timestamp end_time;
  3: double progress = 0;
  4: ExecState state = ExecState.PENDING;
  5: optional list<Task> tasks;
  6: optional map<ExecState, i32> children;
  7: optional list<NamedValue> artifacts;
}

struct Reference {
  1: string step;
  2: string output;
}

union Channel {
  1: string param;
  2: optional Value value;
  3: optional Reference reference;
}

struct NamedChannel {
  1: string name;
  2: Channel channel;
}

struct Export {
  1: string output;
  2: string export_as;
}

struct Step {
  1: string name;
  2: string op;
  3: list<NamedChannel> inputs;
  4: list<Export> exports;
}

/**
 * A run is a particular instantiation of a workflow, where everything is well defined (i.e., all workflow parameters
 * have been affected a value).
 *
 * Runs can be single runs, cloned from another run or belong to a parameter sweep. Clones have their `cloned_from`
 * property defined to the identifier of the run they have been cloned from, and with which they share the same
 * workflow, cluster and environment. Runs that are part of a parameter sweep have their `parent` property set to the
 * identifier of the run that acts as a root for the parameter sweep. All runs that are part of a same parameter sweep
 * share the same workflow, cluster, environment, user and metadata. Parent runs are "dummy" runs that are not
 * actually scheduled, although their state will be updated as the execution of their children progresses.
 *
 * Runs are mostly immutable, only the `state`, `name`, `notes` and `tags` properties are designed to be updated
 * after a run has been created.
 */
struct Job {
  1: string name;

  2: optional User author;

  // Time at which this job has been created.
  3: Timestamp create_time = 0;

  // Human-readable name.
  4: optional string title;

  // Arbitrary tags used when looking for runs.
  5: set<string> tags;

  // Seed used by unstable operators.
  6: i64 seed = 0;

  // Values of workflow parameters. There can possibly be many values for a single parameter, which
  // will cause a parameter sweep to be executed.
  7: list<NamedValue> params;

  // Identifier of the run this instance is a child of.
  8: optional string parent;

  // Identifier of the run this instance has been cloned from.
  9: optional string cloned_from;

  10: list<Step> steps;

  // Execution state.
  12: JobStatus status = {};
}

/**
 * Payload containing everything needed to execute an operator.
 */
struct OpPayload {
  // Name of the operator to execute.
  1: string op;

  // Seed used by unstable operators (included even if the operator is not unstable);
  2: i64 seed;

  // Mapping between a port name and its value. It should contain at least required inputs.
  3: list<NamedValue> params;

  // ComputeResourcess required.
  4: ComputeResources resources;
}

struct OpResult {
  1: bool successful;

  // Arfifacts produced by the operator execution. This should be left empty in case of a failure.
  // If filled, there should be exactly one an artifact per output port of the operator.
  2: list<NamedValue> artifacts;

  // Metrics produced by the operator execution. This can always be filled, whether or not the
  // execution was successful.
  3: list<Metric> metrics;
}