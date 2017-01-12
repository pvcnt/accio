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

package fr.cnrs.liris.accio.testing

import fr.cnrs.liris.accio.core.domain._

/**
 * Static definition of operators to be used in tests.
 */
private[accio] object Operators {
  val FirstSimple: OpDef = createOpDef(
    "FirstSimple",
    Seq(ArgDef("foo", None, DataType(AtomicType.Integer), false)),
    Seq(ArgDef("data", None, DataType(AtomicType.Dataset), false)))
  val SecondSimple: OpDef = createOpDef(
    "SecondSimple",
    Seq(
      ArgDef("dbl", None, DataType(AtomicType.Double), false),
      ArgDef("str", None, DataType(AtomicType.String), false, Some(Value(strings = Seq("something")))),
      ArgDef("data", None, DataType(AtomicType.Dataset), false, None)),
    Seq(ArgDef("data", None, DataType(AtomicType.Dataset), false)))
  val ThirdSimple: OpDef = createOpDef(
    "ThirdSimple",
    Seq(
      ArgDef("data1", None, DataType(AtomicType.Dataset), false, None),
      ArgDef("data2", None, DataType(AtomicType.Dataset), false, None)),
    Seq(ArgDef("data", None, DataType(AtomicType.Dataset), false)))

  val ops: Set[OpDef] = Set(FirstSimple, SecondSimple, ThirdSimple)

  private def createOpDef(name: String, inputs: Seq[ArgDef], outputs: Seq[ArgDef]) = {
    OpDef(
      name = name,
      category = "misc",
      help = None,
      description = None,
      inputs = inputs,
      outputs = outputs,
      deprecation = None,
      resource = Resource(1, 128, 1000))
  }
}
