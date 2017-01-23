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

package fr.cnrs.liris.accio.testing

import fr.cnrs.liris.accio.core.domain._

/**
 * Static definition of operators to be used in tests.
 */
private[accio] object Operators {
  val FirstSimple: OpDef = createOpDef(
    "FirstSimple",
    Seq(ArgDef("foo", DataType(AtomicType.Integer))),
    Seq(ArgDef("data", DataType(AtomicType.Dataset))))
  val SecondSimple: OpDef = createOpDef(
    "SecondSimple",
    Seq(
      ArgDef("dbl", DataType(AtomicType.Double)),
      ArgDef("str", DataType(AtomicType.String), defaultValue = Some(Value(strings = Seq("something")))),
      ArgDef("data", DataType(AtomicType.Dataset))),
    Seq(ArgDef("data", DataType(AtomicType.Dataset))))
  val ThirdSimple: OpDef = createOpDef(
    "ThirdSimple",
    Seq(
      ArgDef("data1", DataType(AtomicType.Dataset)),
      ArgDef("data2", DataType(AtomicType.Dataset))),
    Seq(ArgDef("data", DataType(AtomicType.Dataset))))

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
      unstable = false,
      resource = Resource(1, 128, 1000))
  }
}
