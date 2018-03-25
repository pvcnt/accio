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

package fr.cnrs.liris.accio.runtime.event

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[CollectorEventHandler]].
 */
class CollectorEventHandlerSpec extends UnitSpec {
  behavior of "CollectorEventHandler"

  it should "collect events" in {
    val collector = new CollectorEventHandler
    val events = Seq(Event.start("First task"), Event.warn("Be careful"), Event.info("Bravo!!"))
    events.foreach(collector.handle)
    collector.toSeq should contain theSameElementsInOrderAs events
  }

  it should "filter collected events" in {
    val collector = new CollectorEventHandler(Set(EventKind.Warning, EventKind.Info, EventKind.Finish))
    val events = Seq(Event.start("First task"), Event.warn("Be careful"), Event.info("Bravo!!"))
    events.foreach(collector.handle)
    collector.toSeq should contain theSameElementsInOrderAs events.tail
  }
}