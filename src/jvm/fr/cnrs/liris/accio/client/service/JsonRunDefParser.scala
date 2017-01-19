/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.client.service

import java.io.FileInputStream
import java.nio.file.Path

import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.validation.Min

import scala.util.control.NonFatal

private[service] class JsonRunDefParser @Inject()(mapper: FinatraObjectMapper) {
  @throws[ParsingException]
  def parse(path: Path): JsonRunDef = {
    val file = path.toFile
    if (!file.exists || !file.canRead) {
      throw new ParsingException(s"Cannot read run definition file ${path.toAbsolutePath}")
    }
    val fis = new FileInputStream(file)
    try {
      mapper.parse[JsonRunDef](fis)
    } catch {
      case NonFatal(e) => throw new ParsingException("Error while parsing run definition", e)
    } finally {
      fis.close()
    }
  }
}

private[service] case class JsonRunDef(
  workflow: String,
  name: Option[String],
  notes: Option[String],
  tags: Set[String] = Set.empty,
  seed: Option[Long],
  params: Map[String, Exploration] = Map.empty,
  @Min(1) repeat: Option[Int])

