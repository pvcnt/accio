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

package fr.cnrs.liris.accio.executor

import java.util.UUID

import fr.cnrs.liris.accio.domain.{Operator, Workflow}

import scala.util.Random

final class Compiler(ops: Seq[Operator]) {
  def compile(workflow: Workflow): Seq[Job] = {
    Seq(Job(
      name = UUID.randomUUID().toString,
      params = workflow.params,
      seed = workflow.seed.getOrElse(Random.nextLong()),
      steps = workflow.steps,
      resources = workflow.resources))
  }
}
