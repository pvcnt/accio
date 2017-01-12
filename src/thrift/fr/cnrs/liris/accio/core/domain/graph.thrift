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
 * Definition of where the value of an input comes from.
 */
union InputDef {
  // If the input value comes from the value of a workflow parameter.
  1: optional string param;

  // If the input value comes from the output of another node.
  2: optional common.Reference reference;

  // If the input value is statically fixed.
  3: optional common.Value value;
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