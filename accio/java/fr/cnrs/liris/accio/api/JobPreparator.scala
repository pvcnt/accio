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

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.api.thrift._

import scala.util.Random

@Singleton
final class JobPreparator @Inject()(opRegistry: OpRegistry) {
  def prepare(job: Job, user: Option[String]): Job = {
    // https://stackoverflow.com/questions/4267475/generating-8-character-only-uuids
    val name = if (job.name.isEmpty) UUID.randomUUID().getLeastSignificantBits.toHexString else job.name
    val createTime = if (job.createTime == 0) System.currentTimeMillis() else job.createTime
    val params = job.params.map(prepareParam(_, job))
    val seed = if (job.seed == 0) Random.nextLong() else job.seed
    val steps = prepareSteps(job)
    job.copy(
      name = name,
      createTime = createTime,
      author = user.orElse(job.author),
      params = params,
      parent = None,
      seed = seed,
      steps = steps,
      status = JobStatus())
  }

  private def prepareParam(param: NamedValue, job: Job) = {
    val dataTypes = job.steps.flatMap { step =>
      step.inputs.flatMap { in =>
        in.channel match {
          case thrift.Channel.Param(paramName) if paramName == param.name =>
            opRegistry.get(step.op).toSeq.flatMap { op =>
              op.inputs.find(_.name == in.name).map(_.dataType)
            }
          case _ => Seq.empty
        }
      }
    }.toSet
    if (dataTypes.size == 1) {
      Values.as(param.value, dataTypes.head)
        .map(v => param.copy(value = v))
        .getOrElse(param)
    } else {
      param
    }
  }

  private def prepareSteps(job: Job) = {
    job.steps.map { step =>
      if (step.name.isEmpty) step.copy(name = step.op) else step
    }
  }
}
