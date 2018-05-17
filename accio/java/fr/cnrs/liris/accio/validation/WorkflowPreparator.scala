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

package fr.cnrs.liris.accio.validation

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain._
import fr.cnrs.liris.lumos.domain.AttrValue

import scala.util.Random

@Singleton
final class WorkflowPreparator @Inject()(registry: OpRegistry, nameGenerator: NameGenerator) {
  def prepare(workflow: Workflow, user: Option[String]): Workflow = {
    // https://stackoverflow.com/questions/4267475/generating-8-character-only-uuids
    val name = if (workflow.name.isEmpty) nameGenerator.generateName() else workflow.name
    val params = workflow.params.map(prepareParam(_, workflow))
    val seed = if (workflow.seed == 0) Random.nextLong() else workflow.seed
    val steps = prepareSteps(workflow)
    workflow.copy(
      name = name,
      owner = user.orElse(workflow.owner),
      params = params,
      seed = seed,
      steps = steps)
  }

  private def prepareParam(param: AttrValue, workflow: Workflow): AttrValue = {
    val dataTypes = workflow.steps.flatMap { step =>
      step.params.flatMap {
        case Channel(name, Channel.Param(paramName)) if paramName == param.name =>
          registry.get(step.op).toSeq.flatMap { op =>
            op.inputs.find(_.name == name).map(_.dataType).toSet
          }
        case _ => Set.empty
      }
    }.toSet
    if (dataTypes.size == 1) {
      param.value.cast(dataTypes.head)
        .map(v => param.copy(value = v))
        .getOrElse(param)
    } else {
      param
    }
  }

  private def prepareSteps(workflow: Workflow): Seq[Step] = {
    workflow.steps.map { step =>
      if (step.name.isEmpty) step.copy(name = step.op) else step
    }
  }
}
