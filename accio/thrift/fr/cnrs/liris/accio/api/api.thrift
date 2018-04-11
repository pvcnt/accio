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
  1: required DataType kind;
  2: list<string> strings;
  3: list<i64> longs;
  4: list<double> doubles;
  5: list<i32> integers;
  6: list<bool> booleans;
  7: list<byte> bytes;
  8: required i32 size = 1;
}

struct Artifact {
  // Artifact name.
  1: required string name;

  // Value, that should be consistent with above data type.
  2: required Value value;
}

struct MetricValue {
  // Metric name.
  1: string name;

  // Value.
  2: double value;

  // Unit in which the metric is expressed.
  3: optional string unit;
}

struct User {
  // User name.
  1: required string name;

  // Email address.
  2: optional string email;

  3: set<string> groups;
}

struct Reference {
  1: required string node;
  2: required string port;
}

/**
 * Declaration of resources an operator required to execute.
 */
struct Resource {
  // Fractional number of CPUs.
  1: required double cpus;

  // Quantity of free memory, in MB.
  2: required i64 ram_mb;

  // Quantity of free disk space, in GB.
  3: required i64 disk_gb;
}

/**
 * Definition of an operator port (either input or output).
 */
struct ArgDef {
  // Input name. Should be unique among all inputs of a given operator.
  1: required string name;

  // Data type.
  2: required DataType kind;

  // One-line help text.
  3: optional string help;

  // Default value taken by this input if none is specified. It should be empty for output ports.
  4: optional Value default_value;

  5: required bool is_optional = false;
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

  // Whether this operator is unstable, will produce deterministic outputs given the some inputs. Unstable operators
  // need a random seed specified to produce deterministic outputs.
  9: required bool unstable;

  // Path to the executable implementing this operator.
  10: required string executable;
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
  3: list<Artifact> inputs;

  // Resources required.
  4: Resource resources;
}

/**
 * Result of the execution of an operator. Once again, it includes only information that is reproducible. For
 * example, it means it cannot include information such as started/completed times (but it can include duration).
 * This structure is what can be cached and re-used for another identical operator execution.
 */
struct OpResult {
  // Exit code. It is particularly useful for operators launching external executables and monitoring their execution.
  // For built-in operators, it should still be included. 0 means a successful execution, any other value represents
  // a failed execution.
  1: i32 exit_code;

  // Arfifacts produced by the operator execution. This should be left empty if the exit code indicates a failure.
  // If filled, there should be an artifact per output port of the operator, no more and no less.
  2: list<Artifact> artifacts;

  // Metrics produced by the operator execution. This can always be filled, whether or not the execution is successful.
  // There is no definition of metrics produced by an operator, so any relevant metrics can be included here and will
  // be exposed thereafter.
  3: list<MetricValue> metrics;
}

/**
 * Status of the execution of a task.
 **/
enum TaskState {
  // Waiting for all dependencies to be satisfied.
  WAITING,
  // Submitted to the scheduler.
  SCHEDULED,
  // A task executing this node is running.
  RUNNING,
  // Completed, successfully.
  SUCCESS,
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
struct NodeStatus {
  // Name of the node this state is about.
  1: required string name;

  // Node execution status.
  2: required TaskState status;

  // Time at which the execution of the node started.
  3: optional Timestamp started_at;

  // Time at which the execution of the node completed.
  4: optional Timestamp completed_at;

  // Result of the node execution, either success or failed. Should be filled when the node is completed.
  5: optional OpResult result;

  // Identifier of the task that handled the execution of this node.
  8: optional string task_id;
}

struct RunStatus {
  1: optional Timestamp started_at;
  2: optional Timestamp completed_at;
  3: required double progress;
  4: required TaskState status;
  5: required set<NodeStatus> nodes;
}

struct Package {
  1: required string workflow_id;
  2: optional string workflow_version;
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
struct Run {
  // Run unique identifier.
  1: required string id;

  // Specification of the associated workflow.
  2: required Package pkg;

  // User initiating the run.
  3: optional User owner;

  // Time at which this run has been created.
  4: required Timestamp created_at;

  // Cluster this run belongs to.
  15: required string cluster;

  // Human-readable name.
  6: optional string name;

  // Notes describing the purpose of the run.
  7: optional string notes;

  // Arbitrary tags used when looking for runs.
  8: required set<string> tags;

  // Seed used by unstable operators.
  9: required i64 seed;

  // Values of workflow parameters.
  10: required map<string, Value> params;

  // Identifier of the run this instance is a child of.
  11: optional string parent;

  // Identifiers of children that are parent of this run.
  12: required list<string> children;

  // Identifier of the run this instance has been cloned from.
  13: optional string cloned_from;

  // Execution state.
  14: required RunStatus state;
}

struct Experiment {
  // Specification of the associated workflow.
  1: required Package pkg;

  // User initiating the run.
  2: optional User owner;

  // Human-readable name.
  3: optional string name;

  // Notes describing the purpose of the run.
  4: optional string notes;

  // Arbitrary tags used when looking for runs.
  5: required set<string> tags;

  // Seed used by unstable operators.
  6: optional i64 seed;

  // Values of workflow parameters. There can possibly be many values for a single parameter, which
  // will cause a parameter sweep to be executed.
  7: required map<string, list<Value>> params;

  // Number of times to repeat each run.
  8: optional i32 repeat;

  // Identifier of the run this instance has been cloned from.
  9: optional string cloned_from;
}

union Input {
  1: string param;
  2: Reference reference;
  3: Value value;
}

struct Node {
  1: required string op;
  2: required string name;
  3: required map<string, Input> inputs;
}

/**
 * A workflow is a basically named graph of operators. A workflow can define parameters, which are
 * workflow-level inputs allowing to override the value of some node's inputs at runtime.
 *
 * Workflows are versioned, which allows runs to reference them even if they change afterwards.
 * Version identifiers do not have to be incrementing integers, which allows to use things such as
 * sha1.
 */
struct Workflow {
  // Workflow unique identifier. It is referenced by users creating runs, so it can be a little
  // descriptive and not totally random.
  1: required string id;

  // Version identifier, unique among all version of a particular workflow. Besires this, there is
  // no constraint on it, it is just a plain string.
  2: optional string version;

  // Time at which this version of the workflow was created.
  3: optional Timestamp created_at;

  // Human-readable name.
  4: optional string name;

  // User owning this workflow (usually the one who created it).
  5: optional User owner;

  // Graph definition.
  6: required list<Node> nodes;

  // Workflow parameters.
  7: required list<ArgDef> params;
}

/**
 * Exceptions.
 */
struct FieldViolation {
  // A description of why the value for the field is bad.
  1: required string message;

  // A path leading to a field in the request body, as a dot-separated sequence of field names.
  2: required string field;
}
enum ErrorCode {
  UNKNONWN = 1;
  NOT_FOUND = 2;
  ALREADY_EXISTS = 3;
  UNAUTHENTICATED = 4;
  UNIMPLEMENTED = 5;
  INVALID_ARGUMENT = 6;
  FAILED_PRECONDITION = 7;
}

struct ErrorDetails {
  // A name for the type of resource being accessed.
  1: optional string resource_type;

  // The name of the resource being accessed.
  2: optional string resource_name;

  // A list of errors on the request's fields. These indicate fatal errors that have to be fixed.
  3: list<FieldViolation> errors;

  // A list of warnings on the request's fields. These do not indicate fatal errors, but rather
  // suggestions (e.g., deprecated stuff).
  4: list<FieldViolation> warnings;
}

exception ServerException {
  1: required ErrorCode code;
  2: optional string message;
  3: optional ErrorDetails details;
}
