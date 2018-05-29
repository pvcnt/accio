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

package fr.cnrs.liris.sparkle

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Unit tests for [[SparkleEnv]].
 */
class SparkleEnvSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "SparkleEnv"

  it should "parallelize collections" in {
    (1 to 8).foreach { parallelism =>
      val env = new SparkleEnv(parallelism)
      forAll { items: Seq[Int] =>
        env.parallelize(items).collect() should contain theSameElementsInOrderAs items
      }
    }
  }
}
