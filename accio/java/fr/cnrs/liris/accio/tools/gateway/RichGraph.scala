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

package fr.cnrs.liris.accio.tools.gateway

import fr.cnrs.liris.accio.api.Utils
import fr.cnrs.liris.accio.api.thrift._
import org.joda.time.Instant

case class RichGraph private(steps: Seq[RichGraph.Step])

object RichGraph {

  def apply(job: Job): RichGraph = {
    val steps = job.steps.map { step =>
      val dependencies = job.steps.flatMap { otherStep =>
        otherStep.inputs.flatMap {
          case NamedChannel(_, Channel.Reference(Reference(s, _))) if s == step.name =>
            Some(otherStep.name)
          case _ => None
        }
      }.toSet
      val params = step.inputs.map { case NamedChannel(name, in) =>
        val from = in match {
          case Channel.Reference(ref) => Some(Utils.toString(ref))
          case Channel.Param(paramName) => Some(paramName)
          case _ => None
        }
        val value = in match {
          case Channel.Reference(ref) => job.steps.find(_.name == ref.step).get
          case Channel.Param(paramName) => job.params.find(_.name == paramName).map(_.value)
          case Channel.Value(v) => Some(v)
        }
          null
        //Param(name, value, from)
      }
      Step(
        name = step.name,
        operator = step.op,
        params = params,
        dependencies = dependencies
      )
    }
    RichGraph(steps)
  }

  case class Step(
    name: String,
    operator: String,
    params: Seq[Param],
    dependencies: Set[String])

  case class Param(
    name: String,
    value: Option[Value],
    from: Option[String],
    state: ExecState,
    startTime: Option[Instant],
    endTime: Option[Instant])

}
