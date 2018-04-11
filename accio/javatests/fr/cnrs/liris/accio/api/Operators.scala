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

package fr.cnrs.liris.accio.api

import fr.cnrs.liris.accio.api.thrift._

/**
 * Static definition of operators to be used in tests.
 */
private[accio] object Operators {
  val FirstSimple: Operator = createOperator(
    "FirstSimple",
    Seq(Attribute("foo", DataType.Atomic(AtomicType.Integer))),
    Seq(Attribute("data", DataType.Dataset(DatasetType()))))
  val SecondSimple: Operator = createOperator(
    "SecondSimple",
    Seq(
      Attribute("dbl", DataType.Atomic(AtomicType.Double)),
      Attribute("str", DataType.Atomic(AtomicType.String), defaultValue = Some(Values.encodeString("something"))),
      Attribute("data", DataType.Dataset(DatasetType()))),
    Seq(Attribute("data", DataType.Dataset(DatasetType()))))
  val ThirdSimple: Operator = createOperator(
    "ThirdSimple",
    Seq(
      Attribute("data1", DataType.Dataset(DatasetType())),
      Attribute("data2", DataType.Dataset(DatasetType()))),
    Seq(Attribute("data", DataType.Dataset(DatasetType()))))
  val Deprecated: Operator = createOperator(
    "Deprecated",
    Seq(Attribute("foo", DataType.Atomic(AtomicType.Integer))),
    Seq(Attribute("data", DataType.Dataset(DatasetType()))))
    .copy(deprecation = Some("Do not use it!"))

  val ops: Set[Operator] = Set(FirstSimple, SecondSimple, ThirdSimple, Deprecated)

  private def createOperator(name: String, inputs: Seq[Attribute], outputs: Seq[Attribute]) = {
    Operator(
      name = name,
      executable = ".",
      category = "misc",
      help = None,
      description = None,
      inputs = inputs,
      outputs = outputs,
      deprecation = None,
      unstable = false,
      resource = ComputeResources(1, 128, 1000))
  }
}
