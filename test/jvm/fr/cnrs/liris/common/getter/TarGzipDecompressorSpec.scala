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

package fr.cnrs.liris.common.getter

/**
 * Unit tests for [[TarGzipDecompressor]].
 */
class TarGzipDecompressorSpec extends ArchiveDecompressorSpec {
  protected val decompressor = new TarGzipDecompressor(new TarDecompressor, new GzipDecompressor)

  behavior of "TarGzipDecompressor"

  it should "decompress a single file" in {
    assertSingleFile("fr/cnrs/liris/common/getter/single.tar.gz")
  }

  it should "decompress a tree" in {
    assertTree("fr/cnrs/liris/common/getter/tree.tar.gz")
  }
}
