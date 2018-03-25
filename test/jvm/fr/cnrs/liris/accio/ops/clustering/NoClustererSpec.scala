/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.ops.clustering

import fr.cnrs.liris.accio.ops.testing.WithCabspotting
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[NoClusterer]].
 */
class NoClustererSpec extends UnitSpec with WithCabspotting {
  behavior of "NoClusterer"

  it should "cluster a trace" in {
    val clusters = NoClusterer.cluster(abboipTrace)
    clusters should have size 0
  }
}