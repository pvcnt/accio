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

package fr.cnrs.liris.accio.dsl.json

import fr.cnrs.liris.accio.api.thrift
import fr.cnrs.liris.accio.dsl.JobParser

final class JsonJobParser extends JobParser {
  private[this] def mapper = ObjectMapperFactory.default

  override def parse(content: String): thrift.Job = {
    val obj = mapper.parse[JobDsl](content)
    val steps = obj.steps.map { step =>
      val inputs = step.inputs.map(in => thrift.NamedChannel(in.name, in.channel))
      val exports = step.exports.map(export => thrift.Export(output = export.output, exportAs = export.exportAs))
      thrift.Step(op = step.op, name = step.name.getOrElse(""), inputs = inputs, exports = exports)
    }
    thrift.Job(
      name = obj.name.getOrElse(""),
      title = obj.title,
      tags = obj.tags.toSet,
      seed = obj.seed.getOrElse(0),
      params = obj.params.map(param => thrift.NamedValue(param.name, param.value)),
      steps = steps)
  }
}