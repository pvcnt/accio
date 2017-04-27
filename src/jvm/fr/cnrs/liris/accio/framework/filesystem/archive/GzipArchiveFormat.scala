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

package fr.cnrs.liris.accio.framework.filesystem.archive

import java.io.{InputStream, OutputStream}

import org.apache.commons.compress.compressors.CompressorStreamFactory

/**
 * GZIP archive format.
 */
object GzipArchiveFormat extends AbstractCompressedArchiveFormat {
  override def extensions: Set[String] = Set("gz", "gz2")

  override protected def createInputStream(in: InputStream): InputStream =
    streamFactory.createCompressorInputStream(CompressorStreamFactory.GZIP, in)

  override protected def createOutputStream(out: OutputStream): OutputStream =
    streamFactory.createCompressorOutputStream(CompressorStreamFactory.GZIP, out)
}