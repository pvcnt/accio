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

package fr.cnrs.liris.accio.core.uploader.util

import java.io.{FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path}

import com.google.common.io.ByteStreams
import org.apache.commons.compress.archivers.{ArchiveOutputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory

private[uploader] trait Archiver {
  private[this] val archiveStreamFactory = new ArchiveStreamFactory
  private[this] val compressorStreamFactory = new CompressorStreamFactory

  protected def archiveAndCompress(src: Path): Path = {
    val tarFile = Files.createTempFile("uploader-", ".tar")
    val archive = archiveStreamFactory.createArchiveOutputStream(ArchiveStreamFactory.TAR, new FileOutputStream(tarFile.toFile))
    try {
      addToArchive(src, src, archive)
    } finally {
      archive.close()
    }

    val tarGzFile = Files.createTempFile("uploader-", ".tar.gz")
    val compressed = compressorStreamFactory.createCompressorOutputStream(CompressorStreamFactory.GZIP, new FileOutputStream(tarGzFile.toFile))
    val fis = new FileInputStream(tarFile.toFile)
    try {
      ByteStreams.copy(fis, compressed)
      tarGzFile
    } finally {
      compressed.close()
      fis.close()
    }
  }

  private def addToArchive(root: Path, src: Path, archive: ArchiveOutputStream): Unit = {
    val entryName = src.relativize(root).toString
    val entry = archive.createArchiveEntry(src.toFile, entryName)
    archive.putArchiveEntry(entry)

    if (src.toFile.isDirectory) {
      src.toFile.listFiles.foreach(file => addToArchive(root, file.toPath, archive))
    } else {
      val fis = new FileInputStream(src.toFile)
      try {
        ByteStreams.copy(fis, archive)
      } finally {
        fis.close()
      }
    }
  }
}
