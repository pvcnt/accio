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

package fr.cnrs.liris.accio.core.api

import java.nio.file.Path

/**
 * An operator is the basic processing unit of Accio. It takes a typed input and produces a type output. Input and
 * output types must be case classes, whose fields are one of the allowed types. All operators should be annotated
 * with the [[Op]] annotation.
 *
 * @tparam In  Inputs type
 * @tparam Out Outputs type
 */
trait Operator[In, Out] {
  /**
   * Execute this operator. With given inputs and a given context, it should produce deterministic outputs.
   *
   * Implementations can use a seed if they need to use some randomness. Outside of this, the execution should be
   * perfectly deterministic. A working directory is provided for operators who need to write results somewhere.
   * This is generally a good idea for large that would not fit comfortably in the JVM memory or inside the JSON
   * file report.
   *
   * @param in  Inputs.
   * @param ctx Execution context.
   * @return Outputs.
   */
  def execute(in: In, ctx: OpContext): Out

  /**
   * Specifies whether the outputs of this operator will be unstable under some inputs. Unstable operators need to
   * use a random seed specified in the context to produce deterministic outputs.
   *
   * @param in Inputs.
   * @return True if this execution is unstable, false otherwise.
   */
  def isUnstable(in: In): Boolean = false
}

/**
 * Execution context of an operator.
 *
 * @param _seed    Seed used by an unstable operator, if it is the case.
 * @param workDir  Working directory where data can be written.
 * @param nodeName Name of the node being executed.
 */
class OpContext(_seed: Option[Long], val workDir: Path, val nodeName: String) {
  /**
   * Return the seed to use for an unstable operator.
   *
   * @throws IllegalStateException If the operator is not declared as unstable.
   */
  @throws[IllegalStateException]
  def seed: Long = _seed match {
    case None => throw new IllegalStateException("Operator is not declared as unstable, cannot access the seed")
    case Some(s) => s
  }
}