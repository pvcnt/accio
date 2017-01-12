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
include "fr/cnrs/liris/accio/core/domain/graph.thrift"
include "fr/cnrs/liris/accio/core/domain/operator.thrift"

/**
 * A workflow is a basically named graph of operators. A workflow can define parameters, which are workflow-level
 * inputs allowing to override the value of some node inputs at runtime.
 *
 * Workflows are versioned, which allows runs to reference them even if they change afterwards. Version identifiers
 * do not have to be incrementing integers, which allows to use things such as sha1.
 */
struct Workflow {
  1: required common.WorkflowId id;
  2: required string version;
  // Time at which this version of the workflow was created.
  3: required common.Timestamp created_at;
  4: optional string name;
  5: optional string description;
  6: required common.User owner;
  7: required graph.GraphDef graph;
  // Workflow parameters.
  8: required set<operator.ArgDef> params;
}

struct WorkflowSpec {
  1: required common.WorkflowId id;
  2: optional string version;
  3: optional string name;
  4: optional string description;
  5: optional common.User owner;
  6: required graph.GraphDef graph;
  7: required set<operator.ArgDef> params;
}