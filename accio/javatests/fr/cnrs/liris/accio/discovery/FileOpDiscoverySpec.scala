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

package fr.cnrs.liris.accio.discovery

import java.nio.file.{Files, Paths}

import com.twitter.conversions.time._
import com.twitter.util.Await
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}

/**
 * Unit tests for [[FileOpDiscovery]].
 */
class FileOpDiscoverySpec extends UnitSpec with CreateTmpDirectory {
  behavior of "FileOpDiscovery"

  private val ops0 = Seq(testing.ops(0), testing.ops(1))
  private val ops1 = Seq(testing.ops(2))

  it should "discover operators by descriptors" in {
    copyDescriptors()
    val discovery = new FileOpDiscovery(tmpDir, 0.seconds, None)
    discovery.ops should contain theSameElementsAs ops0 ++ ops1
    Await.result(discovery.close())
  }

  it should "discover operators by binaries" in {
    copyLibraries()
    val discovery = new FileOpDiscovery(tmpDir, 0.seconds, None)
    discovery.ops should contain theSameElementsAs ops0.map(_.copy(executable = RemoteFile(tmpDir.resolve("ops0.jar").toString, Some("application/java-archive")))) ++
      ops1.map(_.copy(executable = RemoteFile(tmpDir.resolve("ops1.jar").toString, Some("application/java-archive"))))
    Await.result(discovery.close())
  }

  it should "filter files by name" in {
    copyDescriptors()
    val discovery = new FileOpDiscovery(tmpDir, 0.seconds, Some("^ops0"))
    discovery.ops should contain theSameElementsAs ops0
    Await.result(discovery.close())
  }

  it should "automatically check for updates" in {
    copyDescriptors()
    val discovery = new FileOpDiscovery(tmpDir, 1.second, Some("^ops0"))

    discovery.ops should contain theSameElementsAs ops0

    Files.copy(tmpDir.resolve("ops1.binthrift"), tmpDir.resolve("ops01.binthrift"))
    Thread.sleep(1200)
    discovery.ops should contain theSameElementsAs ops0 ++ ops1

    Files.delete(tmpDir.resolve("ops0.binthrift"))
    Thread.sleep(1200)
    discovery.ops should contain theSameElementsAs ops1

    Await.result(discovery.close())
  }

  private def copyDescriptors(): Unit = {
    // The files of interest are copied first into a temporary directory. It is indeed safer as we
    // may delete files are part of those test cases.
    Files.copy(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/descriptors/ops0.binthrift"),
      tmpDir.resolve("ops0.binthrift"))
    Files.copy(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/descriptors/ops1.binthrift"),
      tmpDir.resolve("ops1.binthrift"))
  }

  private def copyLibraries(): Unit = {
    // The files of interest are copied first into a temporary directory. It is indeed safer as we
    // may delete files are part of those test cases.
    Files.copy(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/libraries/ops0_deploy.jar"),
      tmpDir.resolve("ops0.jar"))
    Files.copy(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/libraries/ops1_deploy.jar"),
      tmpDir.resolve("ops1.jar"))
  }
}
