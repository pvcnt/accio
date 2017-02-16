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

package fr.cnrs.liris.accio.core.storage.local

import java.io.File
import java.nio.file.{Files, Path}
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.twitter.scrooge.{TArrayByteTransport, ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol.TBinaryProtocol

import scala.collection.mutable

/**
 * Helper methods for repositories storing their data on the local filesystem in Scrooge/Thrift binary files. It
 * is thread-safe.
 *
 * It comes with a locking strategy based on [[ReentrantReadWriteLock]]s. To work correctly, it requires that
 * instances of this class are singletons, in order to use the same locks.
 */
private[local] abstract class LocalRepository {
  private[this] val protocolFactory = new TBinaryProtocol.Factory()
  private[this] val locks = mutable.Map.empty[String, ReentrantReadWriteLock]

  /**
   * Write a Thrift structure inside a file.
   *
   * @param obj  Thrift structure to write.
   * @param file Destination file.
   */
  protected def write(obj: ThriftStruct, file: File): Unit = write(obj, file.toPath)

  /**
   * Write a Thrift structure inside a file.
   *
   * @param obj  Thrift structure to write.
   * @param file Destination file.
   */
  protected def write(obj: ThriftStruct, file: Path): Unit = {
    Files.createDirectories(file.getParent)
    val transport = new TArrayByteTransport
    val protocol = protocolFactory.getProtocol(transport)
    obj.write(protocol)
    val bytes = transport.toByteArray
    withWriteLock(file) {
      Files.write(file, bytes)
    }
  }

  /**
   * Read a Thrift structure from a file.
   *
   * @param file  Source file.
   * @param codec Codec used to read the file.
   * @tparam T Thrift structure type.
   */
  protected def read[T <: ThriftStruct : Manifest](file: File, codec: ThriftStructCodec[T]): Option[T] = read(file.toPath, codec)

  /**
   * Read a Thrift structure from a file.
   *
   * @param file  Source file.
   * @param codec Codec used to read the file.
   * @tparam T Thrift structure type.
   */
  protected def read[T <: ThriftStruct : Manifest](file: Path, codec: ThriftStructCodec[T]): Option[T] = {
    // We must check is file exists before trying to lock on it, because file lock creates the file if it does not
    // exist yet.
    if (file.toFile.exists) {
      val bytes = withReadLock(file) {
        Files.readAllBytes(file)
      }
      val protocol = protocolFactory.getProtocol(TArrayByteTransport(bytes))
      Some(codec.decode(protocol))
    } else {
      None
    }
  }

  private def withReadLock[U](file: Path)(f: => U): U = {
    val lock = getLock(file).readLock
    lock.lock()
    try {
      f
    } finally {
      lock.unlock()
    }
  }

  private def withWriteLock[U](file: Path)(f: => U): U = {
    val lock = getLock(file).writeLock
    lock.lock()
    try {
      f
    } finally {
      lock.unlock()
    }
  }

  private def getLock(file: Path): ReentrantReadWriteLock =
    locks.getOrElseUpdate(file.toAbsolutePath.toString, new ReentrantReadWriteLock)
}