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

import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.locapriv.domain.Event
import fr.cnrs.liris.locapriv.testing.WithTraceGenerator
import fr.cnrs.liris.testing.UnitSpec
import fr.cnrs.liris.util.geo.Distance

/**
 * Unit tests for [[Wait4MeOp]].
 */
class Wait4MeOpSpec extends UnitSpec with WithTraceGenerator with ScalaOperatorSpec {
  behavior of "Wait4MeOp"

  it should "protect mobility trace" in {
    transform(RemoteFile("accio/javatests/fr/cnrs/liris/locapriv/testing"), 2, Distance.meters(500))
  }

  private def transform(data: RemoteFile, k: Int, delta: Distance) = {
    com.twitter.jvm.numProcs.let(1) {
      val res = Wait4MeOp(data = data, k = k, delta = delta).execute(ctx)
      env.read[Event].csv(res.data.uri).collect().toSeq
    }
  }
}
