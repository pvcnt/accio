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

package fr.cnrs.liris.lumos.domain.thrift

import fr.cnrs.liris.lumos.domain
import fr.cnrs.liris.lumos.domain.Arbitraries._
import fr.cnrs.liris.lumos.domain.Generators
import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
 * Unit tests for [[ThriftAdapter]].
 */
class ThriftAdapterSpec extends UnitSpec with GeneratorDrivenPropertyChecks {
  behavior of "ThriftAdapter"

  it should "convert ExecState.state" in {
    forAll { v: domain.ExecStatus.State =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert RemoteFile" in {
    forAll { v: domain.RemoteFile =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert Value" in {
    forAll(Generators.value) { v: domain.Value =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }

    // Unresolved data types.
    ThriftAdapter.toDomain(Value("Unknown", ValuePayload.Int(42))) shouldBe domain.Value.Unresolved(domain.Value.Int(42), "Unknown")
    ThriftAdapter.toThrift(domain.Value.Unresolved(domain.Value.Int(42), "Unknown")) shouldBe Value("Unknown", ValuePayload.Int(42))
  }

  it should "convert AttrValue" in {
    forAll { v: domain.AttrValue =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert ErrorDatum" in {
    forAll { v: domain.ErrorDatum =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert MetricValue" in {
    forAll { v: domain.MetricValue =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert Event" in {
    forAll("event") { v: domain.Event =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }

  it should "convert Job" in {
    forAll("job") { v: domain.Job =>
      ThriftAdapter.toDomain(ThriftAdapter.toThrift(v)) shouldBe v
    }
  }
}
