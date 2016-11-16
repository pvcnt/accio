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

import java.io.{FileInputStream, FileOutputStream}
import java.nio.file.Path

import com.twitter.finatra.json.FinatraObjectMapper

class FileWorkflowRepository(rootPath: Path, mapper: FinatraObjectMapper) extends WorkflowRepository {
  override def list: Seq[String] = rootPath.toFile.list.filter(_.endsWith(".json")).map(_.stripSuffix(".json"))

  override def save(workflow: Workflow): Unit = {
    val fos = new FileOutputStream(getPath(workflow.id).toFile)
    try {
      mapper.writeValue(workflow, fos)
    } finally {
      fos.close()
    }
  }

  override def delete(id: String): Unit = {
    val file = getPath(id).toFile
    if (file.exists) {
      file.delete()
    }
  }

  override def get(id: String): Option[Experiment] = {
    val file = getPath(id).toFile
    if (file.exists) {
      val fis = new FileInputStream(file)
      try {
        Some(mapper.parse[Experiment](fis))
      } finally {
        fis.close()
      }
    } else {
      None
    }
  }

  override def exists(id: String): Boolean = getPath(id).toFile.exists

  private def getPath(id: String) = rootPath.resolve(s"$id.json")
}
