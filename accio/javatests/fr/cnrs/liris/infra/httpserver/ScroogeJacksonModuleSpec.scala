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

package fr.cnrs.liris.infra.httpserver

import com.fasterxml.jackson.databind.ObjectMapper
import fr.cnrs.liris.infra.httpserver.TestEnum.{First, OtherElem, Second}
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[ScroogeJacksonModule]].
 */
class ScroogeJacksonModuleSpec extends UnitSpec {
  behavior of "ScroogeJacksonModule"

  private val objectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(ScroogeJacksonModule)
    mapper
  }

  it should "serialize enums" in {
    objectMapper.writeValueAsString(First) shouldBe "\"First\""
    objectMapper.writeValueAsString(Second) shouldBe "\"Second\""
    objectMapper.writeValueAsString(OtherElem) shouldBe "\"OtherElem\""
  }

  it should "serialize unions" in {
    objectMapper.writeValueAsString(TestUnion.A("foo")) shouldBe """{"a":"foo"}"""
    objectMapper.writeValueAsString(TestUnion.B(42)) shouldBe """{"b":42}"""
    objectMapper.writeValueAsString(TestUnion.C(InnerStruct("bar"))) shouldBe """{"c":{"s":"bar"}}"""
  }

  it should "serialize structs" in {
    objectMapper.writeValueAsString(TestStruct(3, 42, "foo", 3.14, true, InnerStruct("bar"))) shouldBe """{"int":3,"long":42,"str":"foo","dbl":3.14,"b":true,"innerStruct":{"s":"bar"}}"""
  }
}
