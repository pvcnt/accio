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

package fr.cnrs.liris.accio.core.infra.util

import java.io.File
import java.nio.file.{Files, Path}

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct, ThriftStructCodec}
import fr.cnrs.liris.common.util.FileLock
import org.apache.thrift.protocol.TBinaryProtocol

/**
 * Helper methods for repositories storing their data on the local filesystem.
 *
 * @param locking Whether to lock on I/O operations.
 */
private[infra] abstract class LocalStorage(locking: Boolean) {
  private[this] val protocolFactory = new TBinaryProtocol.Factory()

  protected def write(obj: ThriftStruct, file: File): Unit = write(obj, file.toPath)

  protected def write(obj: ThriftStruct, file: Path): Unit = {
    Files.createDirectories(file.getParent)
    val transport = new TArrayByteTransport
    val protocol = protocolFactory.getProtocol(transport)
    obj.write(protocol)
    val bytes = transport.toByteArray
    withLock(file) {
      Files.write(file, bytes)
    }
  }

  protected def read[T: Manifest](file: File, codec: ThriftStructCodec[T]): Option[T] = read(file.toPath, codec)

  protected def read[T: Manifest](file: Path, codec: ThriftStructCodec[T]): Option[T] = {
    // We must check is file exists before trying to lock on it, because file lock creates the file if it does not
    // exist yet.
    if (file.toFile.exists) {
      val bytes = withLock(file) {
        Files.readAllBytes(file)
      }
      val protocol = protocolFactory.getProtocol(TArrayByteTransport(bytes))
      Some(codec.decode(protocol))
    } else {
      None
    }
  }

  private def withLock[U](file: Path)(f: => U): U = {
    val lock = if (locking) Some(new FileLock(file.toFile)) else None
    lock.foreach(_.lock())
    try {
      f
    } finally {
      lock.foreach(_.destroy())
    }
  }
}
