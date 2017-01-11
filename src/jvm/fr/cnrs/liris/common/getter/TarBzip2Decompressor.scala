package fr.cnrs.liris.common.getter

import java.nio.file.Path

import com.google.inject.Inject

class TarBzip2Decompressor @Inject()(tarDecompressor: TarDecompressor, bzipDecompressor: Bzip2Decompressor) extends Decompressor {
  override def decompress(src: Path, dst: Path): Unit = {
    val bzFile = dst.resolveSibling(dst.getFileName.toString + ".bz")
    bzipDecompressor.decompress(src, bzFile)
    tarDecompressor.decompress(bzFile, dst)
    bzFile.toFile.delete()
  }

  override def extensions: Set[String] = Set("tar.bz2", "tar.bz")
}
