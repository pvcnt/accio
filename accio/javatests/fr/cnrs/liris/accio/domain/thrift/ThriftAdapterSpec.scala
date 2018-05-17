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

package fr.cnrs.liris.accio.domain.thrift

import fr.cnrs.liris.accio.domain
import fr.cnrs.liris.accio.domain.Arbitraries._
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Unit tests for [[ThriftAdapter]].
 */
class ThriftAdapterSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "ThriftAdapter"

  it should "convert Operator" in {
    forAll { v: domain.Operator =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert OpPayload" in {
    forAll { v: domain.OpPayload =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert OpResult" in {
    forAll { v: domain.OpResult =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert Workflow" in {
    forAll { v: domain.Workflow =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }
}
