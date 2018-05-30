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

package fr.cnrs.liris.testing

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Path}

import com.google.common.io.{ByteStreams, Resources}
import fr.cnrs.liris.util.FileUtils
import org.scalatest.{BeforeAndAfterEach, Suite}

/**
 * Trait for unit tests that need a temporary directory to be initialized before each test.
 */
trait CreateTmpDirectory extends BeforeAndAfterEach {
  this: Suite =>

  private[this] var _tmpDir: Path = _

  protected def tmpDir: Path = _tmpDir

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    _tmpDir = Files.createTempDirectory(getClass.getSimpleName + '-').toAbsolutePath
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    FileUtils.safeDelete(tmpDir)
    _tmpDir = null
  }

  protected def copyResource(resourceName: String): Path = {
    val dst = tmpDir.resolve(resourceName.split("/").last)
    val in = Resources.asByteSource(Resources.getResource(resourceName)).openBufferedStream()
    val out = new BufferedOutputStream(new FileOutputStream(dst.toFile))
    try {
      ByteStreams.copy(in, out)
      dst
    } finally {
      try {
        in.close()
      } finally {
        out.close()
      }
    }
  }
}