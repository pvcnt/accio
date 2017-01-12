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

package fr.cnrs.liris.accio.core.infrastructure.storage.local

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Files

import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.common.util.FileLock

/**
 * Helper methods for repositories storing their data on the local filesystem.
 *
 * @param mapper  Object mapper.
 * @param locking Whether to lock on I/O operations.
 */
private[local] abstract class LocalRepository(mapper: FinatraObjectMapper, locking: Boolean) {
  protected def write(obj: Any, file: File): Unit = {
    Files.createDirectories(file.toPath.getParent)
    withLock(file) {
      val fos = new FileOutputStream(file)
      try {
        mapper.writeValue(obj, fos)
      } finally {
        fos.close()
      }
    }
  }

  protected def read[T: Manifest](file: File): Option[T] = withLock(file) {
    if (file.exists) {
      val fis = new FileInputStream(file)
      try {
        Some(mapper.parse[T](fis))
      } finally {
        fis.close()
      }
    } else {
      None
    }
  }

  private def withLock[U](file: File)(f: => U): U = {
    val lock = if (locking) Some(new FileLock(file)) else None
    lock.foreach(_.lock())
    try {
      f
    } finally {
      lock.foreach(_.destroy())
    }
  }
}
