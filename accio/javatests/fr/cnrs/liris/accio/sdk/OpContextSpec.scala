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

package fr.cnrs.liris.accio.sdk

import java.nio.file.Paths

import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}

/**
 * Unit tests for [[OpContext]].
 */
class OpContextSpec extends UnitSpec with CreateTmpDirectory {
  behavior of "OpContext"

  it should "provide a seed and a working directory" in {
    val ctx = new OpContext(Some(1234567890L), Paths.get("."))
    ctx.seed shouldBe 1234567890L
    ctx.workDir shouldBe Paths.get(".")
  }

  it should "not provide a seed when not needed" in {
    val ctx = new OpContext(None, Paths.get("."))
    val e = intercept[IllegalStateException](ctx.seed)
    e.getMessage shouldBe "No seed is available"
  }
}