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

namespace java fr.cnrs.liris.accio.core.domain

include "fr/cnrs/liris/dal/core/api/dal.thrift"

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

struct WorkerId {
  1: string value;
}

struct ExecutorId {
  1: string value;
}

struct CacheKey {
  1: string hash;
}

struct Artifact {
  // Artifact name.
  1: required string name;

  // Value, that should be consistent with above data type.
  2: required dal.Value value;
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

struct InvalidSpecMessage {
  1: required string message;
  2: optional string path;
}

/**
 * Declaration of resources an operator required to execute.
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

  // Data type.
  2: required dal.DataType kind;

  // One-line help text.
  3: optional string help;

  // Whether this parameter is optional and does not have to be specified. It should be false for output ports.
  4: required bool is_optional = false;

  // Default value taken by this input if none is specified. It should be empty for output ports.
  5: optional dal.Value default_value;
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
}

/**
 * Payload containing everything needed to execute an operator. This structure embeds only the strict minimum of
 * information to ensure reproducibility of executions. For example, it means it cannot include information such
 * as the node name (the execution of an operator should not depend on the actual node name, only on the operator
 * name). This structure is used to compute a cache key for the result (among other things).
 */
struct OpPayload {
  // Name of the operator to execute.
  1: required string op;

  // Seed used by unstable operators (included even if the operator is not unstable);
  2: required i64 seed;

  // Mapping between a port name and its value. It should contain at least required inputs.
  3: required map<string, dal.Value> inputs;

  // Cache key associated with this payload, used for further memoization.
  4: required CacheKey cache_key;
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
  1: required i32 exit_code;

  // Error captured during the operator execution. This should only be set of the exit code indicates a failure, but
  // it does not have to (e.g., if an operator cannot get structure exception information).
  2: optional Error error;

  // Arfifacts produced by the operator execution. This should be left empty if the exit code indicates a failure.
  // If filled, there should be an artifact per output port of the operator, no more and no less.
  3: required set<Artifact> artifacts;

  // Metrics produced by the operator execution. This can always be filled, whether or not the execution is successful.
  // There is no definition of metrics produced by an operator, so any relevant metrics can be included here and will
  // be exposed thereafter.
  4: required set<Metric> metrics;
}

/**
 * Status of the execution of a node.
 **/
enum NodeStatus {
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
  // Lost, got no heartbeat from the task executing this node.
  LOST,
}

/**
 * State of a node, as part of a run. This structure is regularly updated as the execution of a node progresses.
 */
struct NodeState {
  // Name of the node this state is about.
  1: required string name;

  // Node execution status.
  2: required NodeStatus status;

  // Time at which the execution of the node started.
  3: optional Timestamp started_at;

  // Time at which the execution of the node completed.
  4: optional Timestamp completed_at;

  // Result of the node execution, either success or failed. Should be filled when the node is completed.
  5: optional OpResult result;

  // Cache key used for result memoization. If filled, the `result` field has to be filled too.
  6: optional CacheKey cache_key;

  // Whether it corresponds to a cache hit, i.e., the value has not been explicitly computed. If true, the `result`
  // field has to be filled.
  7: required bool cache_hit = false;
}

/**
 * Status of the execution of a run.
 */
enum RunStatus {
  // Root nodes have been submitted to the scheduler but have not yet started.
  SCHEDULED,
  // Run is active, nodes have been executed or are still running.
  RUNNING,
  // Completed, successfully (i.e., all nodes completed successfully).
  SUCCESS,
  // Completed, but resulted in a failure (i.e., at least one node resulted in a failure).
  FAILED,
  // Killed by the client.
  KILLED,
}

struct RunState {
  1: optional Timestamp started_at;
  2: optional Timestamp completed_at;
  3: required double progress;
  4: required RunStatus status;
  5: required set<NodeState> nodes;
}

struct Package {
  1: required WorkflowId workflow_id;
  2: required string workflow_version;
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
  1: required RunId id;

  // Specification of the associated workflow.
  2: required Package pkg;

  // User initiating the run.
  3: required User owner;

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
  10: required map<string, dal.Value> params;

  // Identifier of the run this instance is a child of.
  11: optional RunId parent;

  // Identifiers of children that are parent of this run.
  12: required list<RunId> children;

  // Identifier of the run this instance has been cloned from.
  13: optional RunId cloned_from;

  // Execution state.
  14: required RunState state;
}

struct RunLog {
  1: required RunId run_id;
  2: required string node_name;
  3: required Timestamp created_at;
  4: required string classifier;
  5: required string message;
}

struct RunSpec {
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

  // Values of workflow parameters. There can possibly be many values for a single parameter, which will cause a
  // parameter sweep to be executed.
  7: required map<string, list<dal.Value>> params;

  // Number of times to repeat each run.
  8: optional i32 repeat;

  // Identifier of the run this instance has been cloned from.
  9: optional RunId cloned_from;
}

/**
 * Definition of where the value of an input comes from.
 */
union InputDef {
  // If the input value comes from the value of a workflow parameter.
  1: string param;

  // If the input value comes from the output of another node.
  2: Reference reference;

  // If the input value is statically fixed.
  3: dal.Value value;
}

/**
 * Definition of a node inside a graph.
 */
struct NodeDef {
  // Operator name.
  1: required string op;

  // Node name.
  2: required string name;

  // Inputs of the operator. Only required inputs (i.e., those non-optional and without a default value) have to be
  // specified here, others can be omitted.
  3: required map<string, InputDef> inputs;
}

/**
 * Definition of a graph.
 */
struct GraphDef {
  // Definition of nodes forming this graph.
  1: required set<NodeDef> nodes;
}

/**
 * A workflow is a basically named graph of operators. A workflow can define parameters, which are workflow-level
 * inputs allowing to override the value of some node inputs at runtime.
 *
 * Workflows are versioned, which allows runs to reference them even if they change afterwards. Version identifiers
 * do not have to be incrementing integers, which allows to use things such as sha1.
 */
struct Workflow {
  // Workflow unique identifier. It is referenced by users creating runs, so it can be a little descriptive and not
  // totally random.
  1: required WorkflowId id;

  // Version identifier, unique among all version of a particular workflow. Besires this, there is no constraint on
  // it, it is just a plain string.
  2: required string version;

  // Whether this object represents the active (i.e., latest) version of the workflow.
  3: required bool is_active;

  // Time at which this version of the workflow was created.
  4: required Timestamp created_at;

  // Human-readable name.
  5: optional string name;

  // User owning this workflow (usually the one who created it).
  6: required User owner;

  // Graph definition.
  7: required GraphDef graph;

  // Workflow parameters.
  8: required set<ArgDef> params;
}

struct WorkflowSpec {
  1: required WorkflowId id;
  2: optional string version;
  3: optional string name;
  4: optional User owner;
  5: required GraphDef graph;
  6: required set<ArgDef> params;
}

struct Task {
  1: required TaskId id;
  2: required RunId run_id;
  3: required string node_name;
  4: required OpPayload payload;
  6: required Timestamp created_at;
  7: required NodeStatus status;
  8: required Resource resource;
}

struct Agent {
  1: required WorkerId id;
  2: optional string dest;
  3: required bool is_master;
  4: required bool is_worker;
  5: required Timestamp registered_at;
  6: required Resource max_resources;
}

/**
 * Exceptions.
 */
exception UnknownRunException {
  1: required RunId id;
  2: optional string message;
}

exception InvalidTaskException {
  1: required TaskId id;
  2: optional string message;
}

exception InvalidSpecException {
  1: required list<InvalidSpecMessage> errors;
  2: required list<InvalidSpecMessage> warnings;
}

exception InvalidExecutorException {
  1: required ExecutorId id;
  2: optional string message;
}

exception InvalidWorkerException {
  1: required WorkerId id;
  2: optional string message;
}