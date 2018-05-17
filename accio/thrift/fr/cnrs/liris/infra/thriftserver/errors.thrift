/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-201 8 Vincent Primault <v.primault@ucl.ac.uk>
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

namespace java fr.cnrs.liris.infra.thriftserver

enum ErrorCode {
  ALREADY_EXISTS,
  NOT_FOUND,
  FAILED_PRECONDITION,
  INVALID_ARGUMENT,
  UNAUTHENTICATED,
  UNIMPLEMENTED,
}

struct FieldViolation {
  1: string message;
  2: string field;
}

exception ServerError {
  1: ErrorCode code;
  2: optional string message;
  3: optional string resource_type;
  4: optional string resource_name;
  5: optional list<FieldViolation> errors;
}