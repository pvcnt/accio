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

package fr.cnrs.liris.util

import fr.cnrs.liris.testing.UnitSpec

class SeqsSpec extends UnitSpec {
  "Seqs" should "compute a cross-product" in {
    val data = Seq(Seq(1, 2), Seq(3, 4, 5), Seq(6, 7, 8))
    Seqs.crossProduct(data) should contain theSameElementsAs Seq(
      Seq(1, 3, 6),
      Seq(1, 4, 6),
      Seq(1, 5, 6),
      Seq(1, 3, 7),
      Seq(1, 4, 7),
      Seq(1, 5, 7),
      Seq(1, 3, 8),
      Seq(1, 4, 8),
      Seq(1, 5, 8),
      Seq(2, 3, 6),
      Seq(2, 4, 6),
      Seq(2, 5, 6),
      Seq(2, 3, 7),
      Seq(2, 4, 7),
      Seq(2, 5, 7),
      Seq(2, 3, 8),
      Seq(2, 4, 8),
      Seq(2, 5, 8))
  }

  it should "compute the cross-product of an empty list" in {
    Seqs.crossProduct(Seq.empty) shouldBe Seq(Seq.empty)
  }
}