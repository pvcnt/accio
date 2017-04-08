/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.api

import java.nio.file.{Files, Paths}

import fr.cnrs.liris.accio.testing.WithSparkleEnv
import fr.cnrs.liris.dal.core.api.Dataset
import fr.cnrs.liris.dal.core.io.IdentityCodec
import fr.cnrs.liris.testing.{UnitSpec, WithTmpDirectory}

/**
 * Unit tests for [[OpContext]].
 */
class OpContextSpec extends UnitSpec with WithSparkleEnv with WithTmpDirectory {
  behavior of "OpContext"

  it should "provide a seed for unstable operators" in {
    val ctx = new OpContext(Some(1234567890L), Paths.get("."), env, Set.empty, Set.empty)
    ctx.seed shouldBe 1234567890L
  }

  it should "not provide a seed for stable operators" in {
    val ctx = new OpContext(None, Paths.get("."), env, Set.empty, Set.empty)
    an[IllegalStateException] shouldBe thrownBy {
      ctx.seed
    }
  }

  it should "read a dataset" in {
    val ctx = new OpContext(None, tmpDir, env, Set(IdentityCodec), Set(IdentityCodec))
    val bytes = "some string that will ultimately be converted into bytes".getBytes
    Files.write(tmpDir.resolve("foo.csv"), bytes)
    Files.write(tmpDir.resolve("bar.csv"), "other content".getBytes)

    val ds = ctx.read[Array[Byte]](Dataset(tmpDir.toString))
    ds.keys should contain theSameElementsAs Set("bar", "foo")
    ds.restrict(Set("foo")).toArray.map(_.deep) should contain theSameElementsAs Set(bytes.deep)
  }
}