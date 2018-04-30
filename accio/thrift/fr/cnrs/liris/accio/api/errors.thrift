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

struct FieldViolation {
  // A description of why the value for the field is bad.
  1: string message;

  // A path leading to a field in the request body, as a dot-separated sequence of field names.
  2: string field;
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
  1: ErrorCode code;
  2: optional string message;
  3: optional ErrorDetails details;
}
