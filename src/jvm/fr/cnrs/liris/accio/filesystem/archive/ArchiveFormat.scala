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

package fr.cnrs.liris.accio.filesystem.archive

import java.io._
import java.nio.file.{Files, Path}

import com.google.common.io.ByteStreams
import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveOutputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.utils.IOUtils

/**
 * A compressed file format can be decompressed.
 */
trait ArchiveFormat {
  /**
   * Source path is guaranteed to exist, destination path is not guaranteed to exist.
   *
   * @param src
   * @param dst
   */
  def decompress(src: Path, dst: Path): Unit

  def compress(src: Path, dst: Path): Unit

  def extensions: Set[String]
}

abstract class AbstractCompressedArchiveFormat extends ArchiveFormat {
  protected val streamFactory = new CompressorStreamFactory

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

  override final def compress(src: Path, dst: Path): Unit = {
    val out = createOutputStream(new FileOutputStream(dst.toFile))
    val in = new FileInputStream(src.toFile)
    try {
      ByteStreams.copy(in, out)
    } finally {
      out.close()
      in.close()
    }
  }

  protected def createInputStream(in: InputStream): InputStream

  protected def createOutputStream(out: OutputStream): OutputStream
}

object ArchiveFormat {
  val available = Set(
    Bzip2ArchiveFormat,
    GzipArchiveFormat,
    TarArchiveFormat,
    TarBzip2ArchiveFormat,
    TarGzipArchiveFormat,
    ZipArchiveFormat)
}

abstract class AbstractArchiveFormat extends ArchiveFormat {
  protected val streamFactory = new ArchiveStreamFactory

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

  override final def compress(src: Path, dst: Path): Unit = {
    val out = createOutputStream(new FileOutputStream(dst.toFile))
    try {
      if (src.toFile.isFile) {
        addToArchive(src.getParent, src, out)
      } else {
        addToArchive(src, src, out)
      }
    } finally {
      out.close()
    }
  }

  protected def createInputStream(in: InputStream): ArchiveInputStream

  protected def createOutputStream(out: OutputStream): ArchiveOutputStream

  private def addToArchive(root: Path, src: Path, archive: ArchiveOutputStream): Unit = {
    val entryName = root.relativize(src).toString
    val entry = archive.createArchiveEntry(src.toFile, entryName)
    archive.putArchiveEntry(entry)

    if (src.toFile.isDirectory) {
      archive.closeArchiveEntry()
      src.toFile.listFiles.foreach(file => addToArchive(root, file.toPath, archive))
    } else {
      val fis = new FileInputStream(src.toFile)
      try {
        ByteStreams.copy(fis, archive)
      } finally {
        fis.close()
      }
      archive.closeArchiveEntry()
    }
  }
}