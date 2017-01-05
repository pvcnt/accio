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

package fr.cnrs.liris.accio.core.framework.local

import java.nio.file.{Files, Path, Paths}

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.api.Dataset
import fr.cnrs.liris.accio.core.framework._
import fr.cnrs.liris.common.util.FileUtils

/**
 * An artifact repository storing artifacts locally. Intended for testing or use in single-node development clusters
 * or with a shared filesystem (e.g., NFS, GlusterFS).
 *
 * @param rootPath Root directory where to store artifacts.
 * @param mapper   Object mapper.
 */
class LocalArtifactRepository(rootPath: Path, mapper: FinatraObjectMapper)
  extends LocalRepository[NodeStatus](mapper, locking = false) with ArtifactRepository {

  override def save(key: NodeKey, status: NodeStatus): Unit = {
    val path = getPath(key)
    Files.createDirectories(path)
    write(rewrite(status, path), path.resolve("status.json").toFile)
  }

  override def get(key: NodeKey): Option[NodeStatus] = {
    read(getPath(key).resolve("status.json").toFile)
  }

  override def delete(key: NodeKey): Unit = {
    FileUtils.safeDelete(getPath(key))
  }

  override def exists(key: NodeKey): Boolean = {
    getPath(key).resolve("status.json").toFile.exists()
  }

  private def getPath(key: NodeKey) = {
    // We create two levels of subdirectories based on node keys to avoid putting to many files in a single
    // directory. Filesystems are not usually very good at dealing with this.
    rootPath
      .resolve(key.value.substring(0, 1))
      .resolve(key.value.substring(1, 2))
      .resolve(key.value)
  }

  private def rewrite(status: NodeStatus, path: Path) = {
    status.copy(artifacts = rewriteArtifacts(status.artifacts, path))
  }

  private def rewriteArtifacts(artifacts: Seq[Artifact], path: Path) = {
    artifacts.map { artifact =>
      artifact.value match {
        case Dataset(uri) =>
          val dest = path.resolve(artifact.name)
          FileUtils.recursiveCopy(Paths.get(uri), dest)
          artifact.copy(value = Dataset(dest.toAbsolutePath.toString))
        case _ => artifact
      }
    }
  }
}