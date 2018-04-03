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

package fr.cnrs.liris.accio.dsl

import java.io.File

import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future
import fr.cnrs.liris.accio.api.thrift

final class ExperimentParser {
  def parse(file: File): Future[thrift.Experiment] = {
    Reader
      .readAll(Reader.fromFile(file))
      .map { case Buf.Utf8(content) => parse(content) }
  }

  def parse(content: String): thrift.Experiment = {
    val json = mapper.parse[ExperimentDsl](content)
    val pkg = thrift.Package(json.workflow)
    thrift.Experiment(
      pkg = pkg,
      owner = None,
      name = json.name,
      notes = json.notes,
      tags = json.tags.toSet,
      seed = json.seed,
      params = json.params.map { case (k, v) => k -> v.values },
      repeat = json.repeat)
  }

  private[this] def mapper = ObjectMapperFactory.default
}