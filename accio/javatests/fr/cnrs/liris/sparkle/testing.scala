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

package fr.cnrs.liris.sparkle

import org.joda.time.Instant

case class TestStruct(i: Int, l: Long, f: Float, d: Double, b: Boolean, s: String, t: Instant)

case class TestParametrizedStruct[T](i: Int, o: T)

case class TestJavaStruct(i: java.lang.Integer, l: java.lang.Long, f: java.lang.Float, d: java.lang.Double, b: java.lang.Boolean, s: java.lang.String)

case class TestInvalidType(t: java.sql.Timestamp)

class OuterClass {

  case class InnerStruct(i: Int)

}

object OuterClass {

  case class InnerStruct2(i: Int)

}