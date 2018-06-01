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

package fr.cnrs.liris.accio.sdk

/**
 * Trait used to help implementing an operator in Scala. Operators should be defined as case
 * classes. The inputs of the operator are materialized as the parameters of this case class,
 * while the outputs are materialized as the parameters of the output it produces, which itself is
 * a case class.
 *
 * Fields of the input and output types must be annotated with the [[Arg]] annotation, while the
 * operator must be annotation with the [[Op]] annotation. Input and output parameters must be
 * supported by one of the registered [[fr.cnrs.liris.lumos.domain.DataType]].
 *
 * @tparam T Output type
 */
trait ScalaOperator[T] {
  this: Product =>
  
  /**
   * Execute this operator. Within provided context, it should produce a deterministic output.
   *
   * Implementations can use a seed if they need some randomness. Outside of this, the execution
   * should be perfectly deterministic. A working directory is provided for operators who need to
   * write results somewhere. This directory is only valid for the operator's life, it can be
   * deleted at any point once the operator completed.
   *
   * @param ctx Execution context.
   */
  def execute(ctx: OpContext): T
}