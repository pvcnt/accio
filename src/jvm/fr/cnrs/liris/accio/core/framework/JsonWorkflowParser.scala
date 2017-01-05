/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.framework

import java.io.{File, FileInputStream}

import com.google.inject.Inject
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.common.util.FileUtils

import scala.util.control.NonFatal

/**
 * Parser for workflow definitions stored into JSON files.
 *
 * @param mapper JSON object mapper.
 */
final class JsonWorkflowParser @Inject()(mapper: FinatraObjectMapper) extends WorkflowParser {
  override def parse(uri: String): WorkflowDef = {
    try {
      val file = getFile(uri)
      mapper.parse[WorkflowDef](new FileInputStream(file))
    } catch {
      case e: IllegalWorkflowException => throw e
      case NonFatal(e) => throw new IllegalWorkflowException(s"JSON syntax error in $uri", e)
    }
  }

  private def getFile(uri: String) = {
    val file = new File(FileUtils.expand(uri))
    if (!file.exists || !file.isFile) {
      throw new IllegalWorkflowException(s"$uri does not seem to be a valid file")
    }
    if (!file.canRead) {
      throw new IllegalWorkflowException(s"$uri is not readable")
    }
    file
  }
}