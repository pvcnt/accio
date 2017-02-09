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

namespace java fr.cnrs.liris.dal.core.api

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