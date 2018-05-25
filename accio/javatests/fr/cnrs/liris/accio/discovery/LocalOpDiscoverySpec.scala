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

import java.nio.file.Paths

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[LocalOpDiscovery]].
 */
class LocalOpDiscoverySpec extends UnitSpec {
  behavior of "LocalOpDiscovery"

  private val ops0 = Seq(testing.ops(0), testing.ops(1))
  private val ops1 = Seq(testing.ops(2))

  it should "discover operators" in {
    val discovery = new LocalOpDiscovery(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/local"),
      None)
    discovery.ops should contain theSameElementsAs ops0 ++ ops1
  }

  it should "filter files by name" in {
    val discovery = new LocalOpDiscovery(
      Paths.get("accio/javatests/fr/cnrs/liris/accio/discovery/local"),
      Some("^ops0"))
    discovery.ops should contain theSameElementsAs ops0
  }
}
