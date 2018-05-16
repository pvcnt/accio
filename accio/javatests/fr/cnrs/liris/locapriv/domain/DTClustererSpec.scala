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
import fr.cnrs.liris.util.geo.{Distance, Point}
import org.joda.time.{Duration, Instant}

/**
 * Unit tests for [[DTClusterer]].
 */
class DTClustererSpec extends UnitSpec with WithCabspotting {
  behavior of "DTClusterer"

  it should "cluster a trace" in {
    val now = Instant.parse("2016-01-01T00:00:00Z")
    val stay1 = Seq(
      Event("Me", Point(1000, 0), now.plus(600 * 1000)),
      Event("Me", Point(1000, 10), now.plus(620 * 1000)),
      Event("Me", Point(1010, 10), now.plus(620 * 1000)),
      Event("Me", Point(1010, 10), now.plus(660 * 1000)))
    val stay2 = Seq(
      Event("Me", Point(1000, 0), now.plus(750 * 1000)),
      Event("Me", Point(1000, 10), now.plus(770 * 1000)),
      Event("Me", Point(1010, 10), now.plus(780 * 1000)),
      Event("Me", Point(1010, 10), now.plus(820 * 1000)))
    val trace = Seq(Event("Me", Point(0, 0), now)) ++
        stay1 ++
        Seq(// Too large diameter.
          Event("Me", Point(1000, 20), now.plus(670 * 1000)),
          Event("Me", Point(1000, 10), now.plus(680 * 1000)),
          Event("Me", Point(1020, 0), now.plus(740 * 1000))) ++
        stay2 ++
        Seq(// Too short duration.
          Event("Me", Point(1020, 20), now.plus(830 * 1000)),
          Event("Me", Point(1020, 20), now.plus(880 * 1000)))
    val clusterer = new DTClusterer(Duration.standardMinutes(1), Distance.meters(15))
    val clusters = clusterer.cluster(trace)
    clusters should have size 2
    clusters.head.events should contain theSameElementsInOrderAs stay1
    clusters.last.events should contain theSameElementsInOrderAs stay2
  }

  it should "create clusters respecting constraints" in {
    val minDuration = Duration.standardMinutes(15)
    val maxDiameter = Distance.meters(100)
    val clusterer = new DTClusterer(minDuration, maxDiameter)
    val clusters = clusterer.cluster(abboipTrace)
    clusters should not be empty
    clusters.foreach { cluster =>
      cluster.duration.getMillis shouldBe >=(minDuration.getMillis)
      cluster.diameter.meters shouldBe <=(maxDiameter.meters)
    }
  }

  it should "be deterministic" in {
    val clusterer = new DTClusterer(Duration.standardMinutes(15), Distance.meters(100))
    val clustersByRun = Seq.fill(5)(clusterer.cluster(abboipTrace))
    (1 until 5).foreach { i =>
      clustersByRun(i) should contain theSameElementsAs clustersByRun.head
    }
  }

  it should "detect a maxDiameter < 0" in {
    val expected = intercept[IllegalArgumentException] {
      new DTClusterer(Duration.standardMinutes(15), Distance.meters(-1))
    }
    expected.getMessage should startWith("requirement failed: maxDiameter must be strictly positive")
  }
}