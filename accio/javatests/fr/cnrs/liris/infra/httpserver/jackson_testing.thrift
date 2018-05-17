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

namespace java fr.cnrs.liris.infra.httpserver

enum TestEnum {
  FIRST,
  SECOND,
  OTHER_ELEM,
}

struct InnerStruct {
  1: string s;
}

struct TestStruct {
  1: i32 int;
  2: i64 long;
  3: string str;
  4: double dbl;
  5: bool b;
  6: InnerStruct inner_struct;
}

union TestUnion {
  1: string a;
  2: i32 b;
  3: InnerStruct c;
}