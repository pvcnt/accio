package fr.cnrs.liris.accio.core.filesystem.archive

import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.core.filesystem.FileSystem
import fr.cnrs.liris.common.util.FileUtils

abstract class ArchivedFileSystem(writeFormat: ArchiveFormat) extends FileSystem {
  override final def read(filename: String, dst: Path): Unit = {
    detectArchive(filename) match {
      case None => doRead(filename, dst)
      case Some(format) =>
        val tmpDir = Files.createTempDirectory(getClass.getSimpleName + "-")
        val tmpArchive = tmpDir.resolve(s"archive.$format")
        doRead(filename, tmpArchive)
        val decompressor = ArchiveFormat.available.find(_.extensions.contains(format)).get
        decompressor.decompress(tmpArchive, dst)
        FileUtils.safeDelete(tmpDir)
    }
  }

  override final def write(src: Path, filename: String): String = {
    val tmpFile = Files.createTempFile(getClass.getSimpleName + "-", writeFormat.extensions.head)
    writeFormat.compress(src, tmpFile)
    doWrite(tmpFile, filename)
  }

  protected def doRead(filename: String, dst: Path): Unit

  protected def doWrite(src: Path, filename: String): String

  private def detectArchive(filename: String) = {
    val archiveExtensions = ArchiveFormat.available.flatMap { format =>
      format.extensions.flatMap { ext =>
        if (filename.endsWith(ext)) {
          Some(ext)
        } else {
          None
        }
      }
    }
    if (archiveExtensions.nonEmpty) {
      Some(archiveExtensions.maxBy(_.length))
    } else {
      None
    }
  }
}