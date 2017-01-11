package fr.cnrs.liris.common.getter

import java.io.InputStream

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

class TarDecompressor extends AbstractArchiveDecompressor {
  override def extensions: Set[String] = Set("tar")

  override protected def createInputStream(in: InputStream): ArchiveInputStream = new TarArchiveInputStream(in)
}