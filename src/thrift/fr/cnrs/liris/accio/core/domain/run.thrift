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

include "fr/cnrs/liris/accio/core/domain/common.thrift"
include "fr/cnrs/liris/accio/core/domain/workflow.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

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
  3: optional common.Timestamp started_at;

  // Time at which the execution of the node completed.
  4: optional common.Timestamp completed_at;

  // Result of the node execution, either success or failed. Should be filled when the node is completed.
  5: optional operator.OpResult result;

  // Cache key used for result memoization. If filled, the `result` field has to be filled too.
  6: optional common.CacheKey cache_key;

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
  1: optional common.Timestamp started_at;
  2: optional common.Timestamp completed_at;
  3: required double progress;
  4: required RunStatus status;
  5: required set<NodeState> nodes;
}

struct Package {
  1: required common.WorkflowId workflow_id;
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
  1: required common.RunId id;

  // Specification of the associated workflow.
  2: required Package pkg;

  // User initiating the run.
  3: required common.User owner;

  // Time at which this run has been created.
  4: required common.Timestamp created_at;

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
  10: required map<string, common.Value> params;

  // Identifier of the run this instance is a child of.
  11: optional common.RunId parent;

  // Identifiers of children that are parent of this run.
  12: required list<common.RunId> children;

  // Identifier of the run this instance has been cloned from.
  13: optional common.RunId cloned_from;

  // Execution state.
  14: required RunState state;
}

struct RunLog {
  1: required common.RunId run_id;
  2: required string node_name;
  3: required common.Timestamp created_at;
  4: required string classifier;
  5: required string message;
}

struct RunSpec {
  // Specification of the associated workflow.
  1: required Package pkg;

  // User initiating the run.
  2: optional common.User owner;

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
  7: required map<string, list<common.Value>> params;

  // Number of times to repeat each run.
  8: optional i32 repeat;

  // Identifier of the run this instance has been cloned from.
  9: optional common.RunId cloned_from;
}