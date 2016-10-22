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

/**
 * Parser for workflow definitions stored into JSON files.
 *
 * @param mapper JSON object mapper.
 */
class JsonWorkflowParser @Inject()(mapper: FinatraObjectMapper) extends WorkflowParser {
  override def parse(uri: String): WorkflowDef = {
    val file = getFile(uri)
    mapper.parse[WorkflowDef](new FileInputStream(file))
  }

  override def canRead(uri: String): Boolean = {
    val file = getFile(uri)
    mapper.objectMapper.readTree(file).has("graph")
  }

  private def getFile(uri: String) = {
    val file = new File(FileUtils.replaceHome(uri))
    require(file.exists && file.isFile, s"${file.getAbsolutePath} does not seem to be a valid file")
    require(file.canRead, s"${file.getAbsolutePath} is not readable")
    file
  }
}