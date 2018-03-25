/*
 * Accio is a program whose purpose is to study location privacy.
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
 * An operator is the basic processing unit of Accio. It takes a typed input and produces a typed output. Input and
 * output types must be case classes, whose fields are one of the allowed types. All operators should be annotated
 * with the [[Op]] annotation.
 *
 * @tparam In  Input type
 * @tparam Out Output type
 */
trait Operator[In, Out] {
  /**
   * Execute this operator. With given input and context, it should produce a deterministic output.
   *
   * Implementations can use a seed if they need some randomness. Outside of this, the execution should be
   * perfectly deterministic. A working directory is provided for operators who need to write results
   * somewhere. This directory is only valid for the operator's life, it can be deleted at any point once
   * the operator completed.
   *
   * @param in  Input.
   * @param ctx Execution context.
   */
  def execute(in: In, ctx: OpContext): Out
}