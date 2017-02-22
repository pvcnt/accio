package fr.cnrs.liris.accio.core.filesystem.archive

import java.io.{InputStream, OutputStream}

import org.apache.commons.compress.archivers.{ArchiveInputStream, ArchiveOutputStream, ArchiveStreamFactory}

object TarArchiveFormat extends AbstractArchiveFormat {
  override def extensions: Set[String] = Set("tar")

  override protected def createInputStream(in: InputStream): ArchiveInputStream =
    streamFactory.createArchiveInputStream(ArchiveStreamFactory.TAR, in)

  override protected def createOutputStream(out: OutputStream): ArchiveOutputStream =
    streamFactory.createArchiveOutputStream(ArchiveStreamFactory.TAR, out)
}