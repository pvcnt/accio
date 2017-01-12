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
include "fr/cnrs/liris/accio/core/domain/workflow.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

enum NodeStatus {
  WAITING,
  SCHEDULED,
  RUNNING,
  SUCCESS,
  FAILED,
  KILLED,
  CANCELLED,
}

struct NodeState {
  1: required string node_name;
  2: optional common.Timestamp started_at;
  3: optional common.Timestamp completed_at;
  4: required double progress;
  5: required NodeStatus status;
  6: required set<common.TaskId> task_ids;
  7: optional operator.OpResult result;
}

enum RunStatus {
  SCHEDULED,
  RUNNING,
  SUCCESS,
  FAILED,
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
 * Runs can be single runs, cloned from another run or belong to a parameter sweep. Clones have their `clonedFrom`
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

  // Cluster providing resources.
  3: required string cluster;

  // Environment.
  4: required string environment;

  // User initiating the run.
  5: required common.User owner;

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

  // Identifiers of the runs this instance is a parent of.
  12: optional set<common.RunId> children;

  // Identifier of the run this instance has been cloned from.
  13: optional common.RunId cloned_from;

  // Time at which this run has been created.
  14: required common.Timestamp created_at;

  // Execution state.
  15: required RunState state;
}

struct RunTemplate {
  // Specification of the associated workflow.
  1: required string pkg;

  // Cluster providing resources to execute the run.
  2: required string cluster;

  // Environment inside which the run is executed.
  3: optional string environment;

  // User initiating the run.
  5: optional common.User owner;

  // Human-readable name.
  6: optional string name;

  // Notes describing the purpose of the run.
  7: optional string notes;

  // Arbitrary tags used when looking for runs.
  8: required set<string> tags;

  // Seed used by unstable operators.
  9: optional i64 seed;

  // Values of workflow parameters. There can possibly be many values for a single parameter, which will cause a
  // parameter sweep to be executed.
  11: required map<string, list<common.Value>> params;

  // Number of times to repeat each run.
  12: optional i32 repeat;

  // Identifier of the run this instance has been cloned from.
  13: optional common.RunId cloned_from;
}