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

package fr.cnrs.liris.accio.runtime.event

import java.io.PrintWriter

import fr.cnrs.liris.testing.UnitSpec
import org.scalatest.BeforeAndAfterEach

/**
 * Unit tests for [[ReporterStream]].
 */
class ReporterStreamSpec extends UnitSpec with BeforeAndAfterEach {
  behavior of "ReporterStream"

  private var out: StringBuilder = null
  private var outAppender: EventHandler = null

  override protected def beforeEach() = {
    out = new StringBuilder
    outAppender = new EventHandler {
      override def handle(event: Event): Unit = {
        out.append(s"[${event.kind}: ${event.message}]\n")
      }
    }
  }

  override protected def afterEach() = {
    outAppender = null
    out = null
  }

  it should "write to stream" in {
    val reporter = new Reporter(Seq(outAppender))
    val warnWriter = new PrintWriter(new ReporterStream(reporter, EventKind.Warning), true)
    val infoWriter = new PrintWriter(new ReporterStream(reporter, EventKind.Info), true)
    try {
      infoWriter.println("some info")
      warnWriter.println("a warning")
    } finally {
      warnWriter.close()
      infoWriter.close()
    }
    reporter.outErr.printOutLn("some output")
    reporter.outErr.printErrLn("an error")
    out.toString shouldBe "[Info: some info\n]\n[Warning: a warning\n]\n[Stdout: some output\n]\n[Stderr: an error\n]\n"
  }
}
