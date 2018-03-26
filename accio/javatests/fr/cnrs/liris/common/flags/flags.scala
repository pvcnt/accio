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

package fr.cnrs.liris.common.flags

case class BazFlags(
  @Flag(name = "baz", category = "one")
  baz: String = "defaultBaz")

case class CategoryTestFlags(
  @Flag(name = "swiss_bank_account_number", category = "undocumented")
  swissBankAccountNumber: Int = 123456789,
  @Flag(name = "student_bank_account_number", category = "one")
  studentBankAccountNumber: Int = 987654321)

case class FooFlags(
  @Flag(name = "foo", category = "one")
  foo: String = "defaultFoo",
  @Flag(name = "bar", category = "two")
  bar: Int = 42,
  @Flag(name = "nodoc", category = "undocumented")
  nodoc: String = "")

case class OptionalFlags(
  @Flag(name = "foo") foo: Option[Int],
  @Flag(name = "bar") bar: Option[String],
  @Flag(name = "baz") baz: Option[String] = Some("bazbaz"))
