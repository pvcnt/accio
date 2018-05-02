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

package fr.cnrs.liris.locapriv.domain

import fr.cnrs.liris.locapriv.testing.WithCabspotting
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance
import org.joda.time.Duration

/**
 * Unit tests for [[IdentityClusterer]].
 */
class IdentityClustererSpec extends UnitSpec with WithCabspotting {
  behavior of "IdentityClusterer"

  it should "cluster a trace" in {
    val clusters = IdentityClusterer.cluster(abboipTrace)
    clusters should have size abboipTrace.size
    clusters.foreach { cluster =>
      cluster.events should have size 1
    }
  }

  it should "be deterministic" in {
    val clusterer = new DTClusterer(Duration.standardMinutes(15), Distance.meters(100))
    val clustersByRun = Seq.fill(5)(clusterer.cluster(abboipTrace))
    (1 until 5).foreach { i =>
      clustersByRun(i) should contain theSameElementsAs clustersByRun.head
    }
  }
}