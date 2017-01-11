package fr.cnrs.liris.common.getter

import java.io.InputStream

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream

class ZipDecompressor extends AbstractArchiveDecompressor {
  override def extensions: Set[String] = Set("zip")

  override protected def createInputStream(in: InputStream): ArchiveInputStream = new ZipArchiveInputStream(in)
}