package fr.cnrs.liris.common.getter

import java.nio.file.Path

import com.google.inject.Inject

class TarGzipDecompressor @Inject()(tarDecompressor: TarDecompressor, gzipDecompressor: GzipDecompressor) extends Decompressor {
  override def decompress(src: Path, dst: Path): Unit = {
    val tarFile = dst.resolveSibling(dst.getFileName.toString + ".tar")
    gzipDecompressor.decompress(src, tarFile)
    tarDecompressor.decompress(tarFile, dst)
    tarFile.toFile.delete()
  }

  override def extensions: Set[String] = Set("tar.gz")
}
