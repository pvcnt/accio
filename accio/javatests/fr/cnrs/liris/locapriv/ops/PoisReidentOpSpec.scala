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

package fr.cnrs.liris.locapriv.ops

import fr.cnrs.liris.locapriv.domain.{Event, Poi}
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Point

/**
 * Unit tests for [[PoisReidentOp]].
 */
class PoisReidentOpSpec extends UnitSpec with ScalaOperatorSpec with WithTraceGenerator {
  behavior of "PoisReidentOp"

  private[this] def trainPois = Seq(
      Poi(Set(Event("user1", Point(5, 5), Now))),
      Poi(Set(Event("user1", Point(0, 0), Now))),
      Poi(Set(Event("user2", Point(10, 8), Now))),
      Poi(Set(Event("user2", Point(1, 1), Now))),
      Poi(Set(Event("user3", Point(8, 6), Now))),
      Poi(Set(Event("user3", Point(-2, -9), Now))))

  it should "identify all users when ran on the same data" in {
    val res = execute(trainPois, trainPois)
    val metrics = env.read[PoisReidentOp.Value].csv(res.metrics.uri).collect().toSeq
    metrics should contain theSameElementsAs Seq(
      PoisReidentOp.Value("user1", "user1", 0),
      PoisReidentOp.Value("user2", "user2", 0),
      PoisReidentOp.Value("user3", "user3", 0))
    res.rate shouldBe 1
  }

  it should "identify users" in {
    val testPois = Seq(
      Poi(Set(Event("user1", Point(4, 4), Now))),
      Poi(Set(Event("user2", Point(9, 7), Now))),
      Poi(Set(Event("user2", Point(-1, -10), Now))),
      Poi(Set(Event("user3", Point(9, 7), Now))),
      Poi(Set(Event("user3", Point(2, 2), Now))))
    val res = execute(trainPois, testPois)

    val metrics = env.read[PoisReidentOp.Value].csv(res.metrics.uri).collect().toSeq
    metrics should contain theSameElementsAs Seq(
      PoisReidentOp.Value("user1", "user1", 1.414213562457019),
      PoisReidentOp.Value("user2", "user3", 1.4142135619571015),
      PoisReidentOp.Value("user3", "user2", 1.4142135617700253))
    res.rate shouldBe closeTo(1d / 3, 0.001)
  }

  private def execute(train: Seq[Poi], test: Seq[Poi]): PoisReidentOp.Out = {
    val trainDs = writePois(train)
    val testDs = writePois(test)
    com.twitter.jvm.numProcs.let(1) {
      PoisReidentOp(trainDs, testDs).execute(ctx)
    }
  }
}