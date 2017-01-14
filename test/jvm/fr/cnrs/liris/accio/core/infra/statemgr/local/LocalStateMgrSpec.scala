/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.infra.statemgr.local

import java.nio.file.{Files, Path}

import fr.cnrs.liris.accio.core.infra.statemgr.StateMgrSpec
import fr.cnrs.liris.common.util.FileUtils
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests of [[LocalStateMgr]].
 */
class LocalStateMgrSpec extends StateMgrSpec with BeforeAndAfterEach {
  private[this] var tmpDir: Path = null

  override protected def beforeEach(): Unit = {
    tmpDir = Files.createTempDirectory("accio-test-")
  }

  override protected def afterEach(): Unit = {
    FileUtils.safeDelete(tmpDir)
    tmpDir = null
  }

  protected def createStateMgr = new LocalStateMgr(tmpDir)

  behavior of "LocalStateMgr"
}