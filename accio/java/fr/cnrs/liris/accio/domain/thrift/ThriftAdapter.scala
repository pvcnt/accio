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

package fr.cnrs.liris.accio.domain.thrift

import fr.cnrs.liris.accio.domain
import fr.cnrs.liris.lumos.domain.DataType
import fr.cnrs.liris.lumos.domain.thrift.{ThriftAdapter => LumosAdapter}

object ThriftAdapter {
  def toDomain(obj: OpPayload): domain.OpPayload = {
    domain.OpPayload(
      op = obj.op,
      seed = obj.seed,
      params = obj.params.map(LumosAdapter.toDomain),
      resources = obj.resources.toMap)
  }

  def toDomain(obj: OpResult): domain.OpResult = {
    domain.OpResult(
      successful = obj.successful,
      artifacts = obj.artifacts.map(LumosAdapter.toDomain),
      metrics = obj.metrics.map(LumosAdapter.toDomain),
      error = obj.error.map(LumosAdapter.toDomain))
  }

  def toDomain(obj: Operator): domain.Operator = {
    domain.Operator(
      name = obj.name,
      category = obj.category,
      executable = LumosAdapter.toDomain(obj.executable),
      help = obj.help,
      description = obj.description,
      inputs = obj.inputs.map(toDomain),
      outputs = obj.outputs.map(toDomain),
      deprecation = obj.deprecation,
      unstable = obj.unstable)
  }

  def toDomain(obj: Workflow): domain.Workflow = {
    domain.Workflow(
      name = obj.name,
      owner = obj.owner,
      contact = obj.contact,
      labels = obj.labels.toMap,
      seed = obj.seed,
      params = obj.params.map(LumosAdapter.toDomain),
      steps = obj.steps.map(toDomain),
      repeat = obj.repeat,
      resources = obj.resources.toMap)
  }

  private def toDomain(obj: Step): domain.Step = {
    domain.Step(name = obj.name, op = obj.op, params = obj.params.map(toDomain))
  }

  private def toDomain(obj: Channel): domain.Channel = {
    domain.Channel(name = obj.name, source = toDomain(obj.source))
  }

  private def toDomain(obj: Source): domain.Channel.Source =
    obj match {
      case Source.Param(name) => domain.Channel.Param(name)
      case Source.Reference(ref) => domain.Channel.Reference(ref.step, ref.output)
      case Source.Constant(value) => domain.Channel.Constant(LumosAdapter.toDomain(value))
      case Source.UnknownUnionField(_) => throw new IllegalArgumentException("Illegal value")
    }

  private def toDomain(obj: Attribute): domain.Attribute = {
    val dataType = DataType.parse(obj.dataType).getOrElse {
      throw new IllegalArgumentException(s"Unknown data type: ${obj.dataType}")
    }
    domain.Attribute(
      name = obj.name,
      dataType = dataType,
      help = obj.help,
      defaultValue = obj.defaultValue.map(LumosAdapter.toDomain),
      optional = obj.isOptional,
      aspects = obj.aspects.toSet)
  }

  def toThrift(obj: domain.OpPayload): OpPayload = {
    OpPayload(
      op = obj.op,
      seed = obj.seed,
      params = obj.params.map(LumosAdapter.toThrift),
      resources = obj.resources)
  }

  def toThrift(obj: domain.OpResult): OpResult = {
    OpResult(
      successful = obj.successful,
      artifacts = obj.artifacts.map(LumosAdapter.toThrift),
      metrics = obj.metrics.map(LumosAdapter.toThrift),
      error = obj.error.map(LumosAdapter.toThrift))
  }

  def toThrift(obj: domain.Workflow): Workflow = {
    Workflow(
      name = obj.name,
      owner = obj.owner,
      contact = obj.contact,
      labels = obj.labels,
      seed = obj.seed,
      params = obj.params.map(LumosAdapter.toThrift),
      steps = obj.steps.map(toThrift),
      repeat = obj.repeat,
      resources = obj.resources)
  }

  private def toThrift(obj: domain.Step): Step = {
    Step(name = obj.name, op = obj.op, params = obj.params.map(toThrift))
  }

  private def toThrift(obj: domain.Channel): Channel = {
    Channel(name = obj.name, source = toThrift(obj.source))
  }

  private def toThrift(obj: domain.Channel.Source): Source =
    obj match {
      case domain.Channel.Param(name) => Source.Param(name)
      case domain.Channel.Reference(step, output) => Source.Reference(Reference(step, output))
      case domain.Channel.Constant(value) => Source.Constant(LumosAdapter.toThrift(value))
    }

  def toThrift(obj: domain.Operator): Operator = {
    Operator(
      name = obj.name,
      category = obj.category,
      executable = LumosAdapter.toThrift(obj.executable),
      help = obj.help,
      description = obj.description,
      inputs = obj.inputs.map(toThrift),
      outputs = obj.outputs.map(toThrift),
      deprecation = obj.deprecation,
      unstable = obj.unstable)
  }

  private def toThrift(obj: domain.Attribute): Attribute = {
    Attribute(
      name = obj.name,
      dataType = obj.dataType.name,
      help = obj.help,
      defaultValue = obj.defaultValue.map(LumosAdapter.toThrift),
      isOptional = obj.optional,
      aspects = obj.aspects)
  }
}
