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

package fr.cnrs.liris.lumos.transport

import com.google.inject.Module
import com.twitter.inject.CreateTwitterInjector
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[EventTransportModule]].
 */
class EventTransportModuleSpec extends UnitSpec with CreateTwitterInjector {
  behavior of "EventTransportModule"

  override protected def modules: Seq[Module] = Seq(EventTransportModule)

  it should "provide a default transport" in {
    val injector = createInjector()
    injector.instance[EventTransport] shouldBe a[EventTransportMultiplexer]
    injector.instance[EventTransportMultiplexer].name shouldBe "Multiplexer[]"

    EventTransportModule.args should have size 0
  }

  it should "provide a transport with binary file support" in {
    val injector = createInjector("-event.binary.file", "/dev/null")
    injector.instance[EventTransport] shouldBe a[EventTransportMultiplexer]
    injector.instance[EventTransportMultiplexer].name shouldBe "Multiplexer[BinaryFile]"

    EventTransportModule.args should contain theSameElementsAs Seq("-event.binary.file", "/dev/null")
  }

  it should "provide a transport with text file support" in {
    val injector = createInjector("-event.text.file", "/dev/null")
    injector.instance[EventTransport] shouldBe a[EventTransportMultiplexer]
    injector.instance[EventTransportMultiplexer].name shouldBe "Multiplexer[TextFile]"

    EventTransportModule.args should contain theSameElementsAs Seq("-event.text.file", "/dev/null")
  }

  it should "provide a transport with Lumos service support" in {
    val injector = createInjector("-event.server.address", "localhost:9999")
    injector.instance[EventTransport] shouldBe a[EventTransportMultiplexer]
    injector.instance[EventTransportMultiplexer].name shouldBe "Multiplexer[LumosService]"

    EventTransportModule.args should contain theSameElementsAs Seq("-event.server.address", "localhost:9999")
  }

  it should "provide a transport with multiple supports" in {
    val injector = createInjector(
      "-event.binary.file", "/dev/null",
      "-event.text.file", "/dev/null",
      "-event.server.address", "localhost:9999")
    injector.instance[EventTransport] shouldBe a[EventTransportMultiplexer]
    injector.instance[EventTransportMultiplexer].name shouldBe "Multiplexer[BinaryFile,TextFile,LumosService]"

    EventTransportModule.args should contain theSameElementsAs Seq(
      "-event.binary.file", "/dev/null",
      "-event.text.file", "/dev/null",
      "-event.server.address", "localhost:9999")
  }
}