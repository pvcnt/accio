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

package fr.cnrs.liris.common.getter

import java.io._
import java.nio.file.{Files, Path}

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.utils.IOUtils

/**
 * A compressed file format can be decompressed.
 */
trait Decompressor {
  /**
   *
   * Source path is guaranteed to exist, destination path is not guaranteed to exist.
   *
   * @param src
   * @param dst
   */
  def decompress(src: Path, dst: Path): Unit

  def extensions: Set[String]
}

abstract class AbstractFileDecompressor extends Decompressor {
  override final def decompress(src: Path, dst: Path): Unit = {
    Files.createDirectories(dst.getParent)
    val in = createInputStream(new BufferedInputStream(new FileInputStream(src.toFile)))
    val out = new FileOutputStream(dst.toFile)
    try {
      IOUtils.copy(in, out)
    } finally {
      try {
        in.close()
      } finally {
        out.close()
      }
    }
  }

  protected def createInputStream(in: InputStream): InputStream
}

abstract class AbstractArchiveDecompressor extends Decompressor {
  override final def decompress(src: Path, dst: Path): Unit = {
    Files.createDirectories(dst)
    val in = createInputStream(new BufferedInputStream(new FileInputStream(src.toFile)))
    try {
      var entry = in.getNextEntry
      while (null != entry) {
        val file = dst.resolve(entry.getName).toFile
        if (entry.isDirectory) {
          file.mkdirs()
        } else {
          file.getParentFile.mkdirs()
          val out = new BufferedOutputStream(new FileOutputStream(file))
          try {
            IOUtils.copy(in, out)
          } finally {
            out.close()
          }
        }
        entry = in.getNextEntry
        //TODO: propagate file permissions.
      }
    } finally {
      in.close()
    }
  }

  protected def createInputStream(in: InputStream): ArchiveInputStream
}