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

namespace java fr.cnrs.liris.accio.domain.thrift

include "accio/thrift/fr/cnrs/liris/lumos/domain/lumos.thrift"

struct Reference {
  1: string step;
  2: string output;
}

union Source {
  1: string param;
  2: optional lumos.Value constant;
  3: optional Reference reference;
}

struct Channel {
  1: string name;
  2: Source source;
}

struct Step {
  1: string name;
  2: string op;
  3: list<Channel> params;
}

struct Workflow {
  1: string name;
  2: optional string owner;
  3: optional string contact;
  4: map<string, string> labels;
  5: i64 seed = 0;
  6: list<lumos.AttrValue> params;
  7: list<Step> steps;
  8: i32 repeat = 1;
  9: map<string, i64> resources;
}

struct OpPayload {
  1: string op;
  2: i64 seed;
  3: list<lumos.AttrValue> params;
  4: map<string, i64> resources;
}

struct OpResult {
  1: bool successful;
  2: list<lumos.AttrValue> artifacts;
  3: list<lumos.MetricValue> metrics;
  4: optional lumos.ErrorDatum error;
}

struct Attribute {
  1: string name;
  2: string data_type;
  3: optional string help;
  4: optional lumos.Value default_value;
  5: bool is_optional = false;
  6: set<string> aspects;
}

struct Operator {
  1: string name;
  2: string category;
  3: lumos.RemoteFile executable;
  4: optional string help;
  5: optional string description;
  6: list<Attribute> inputs;
  7: list<Attribute> outputs;
  8: optional string deprecation;
  // 9: removed
  10: bool unstable;
}