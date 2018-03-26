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

package fr.cnrs.liris.accio.tools.cli.event

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[Reporter]].
 */
class ReporterSpec extends UnitSpec {
  behavior of "Reporter"

  it should "show output" in {
    val collector = new CollectorEventHandler
    val reporter = new Reporter(Seq(collector), new RegexOutputFilter("naughty".r))
    val interesting = Event(EventKind.Warning, "show-me", Some("naughty"))

    reporter.handle(interesting)
    reporter.handle(Event(EventKind.Warning, "ignore-me", Some("good")))

    collector.toSeq should contain theSameElementsInOrderAs Seq(interesting)
  }

  it should "collect events" in {
    val want = Seq(Event.warn("xyz"), Event.error("err"))
    val collector = new CollectorEventHandler
    val reporter = new Reporter(Seq(collector), new RegexOutputFilter("naughty".r))
    want.foreach(reporter.handle)

    collector.toSeq should contain theSameElementsInOrderAs want
  }
}