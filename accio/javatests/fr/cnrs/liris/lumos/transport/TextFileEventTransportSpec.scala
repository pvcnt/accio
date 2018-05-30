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

import com.twitter.util.Await
import fr.cnrs.liris.lumos.domain.{Event, Job}
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}
import org.joda.time.Instant

import scala.io.Source

/**
 * Unit tests for [[TextFileEventTransport]].
 */
class TextFileEventTransportSpec extends UnitSpec with CreateTmpDirectory {
  behavior of "TextFileEventTransport"

  it should "write events" in {
    val file = tmpDir.resolve("events.txt")
    val transport = new TextFileEventTransport(file)
    val time = 123456789012L
    transport.sendEvent(Event("foo", 0, new Instant(time), Event.JobEnqueued(Job(name = "foo"))))
    transport.sendEvent(Event("foo", 1, new Instant(time), Event.JobStarted(message = Some("started"))))
    transport.sendEvent(Event("bar", 0, new Instant(time), Event.JobEnqueued(Job(name = "bar"))))
    transport.sendEvent(Event("foo", 2, new Instant(time), Event.JobCompleted(message = Some("completed"))))
    Await.result(transport.close())

    val content = Source.fromFile(file.toFile).mkString.trim
    content shouldBe """{
                       |  parent = foo
                       |  sequence = 0
                       |  time = 123456789012
                       |  payload = {
                       |    job_enqueued = {
                       |      job = {
                       |        name = foo
                       |        create_time = 0
                       |        labels = {
                       |        }
                       |        metadata = {
                       |        }
                       |        inputs = [
                       |        ]
                       |        outputs = [
                       |        ]
                       |        progress = 0
                       |        tasks = [
                       |        ]
                       |        status = {
                       |          state = 0
                       |          time = 0
                       |        }
                       |        history = [
                       |        ]
                       |      }
                       |    }
                       |  }
                       |}
                       |{
                       |  parent = foo
                       |  sequence = 1
                       |  time = 123456789012
                       |  payload = {
                       |    job_started = {
                       |      message = started
                       |    }
                       |  }
                       |}
                       |{
                       |  parent = bar
                       |  sequence = 0
                       |  time = 123456789012
                       |  payload = {
                       |    job_enqueued = {
                       |      job = {
                       |        name = bar
                       |        create_time = 0
                       |        labels = {
                       |        }
                       |        metadata = {
                       |        }
                       |        inputs = [
                       |        ]
                       |        outputs = [
                       |        ]
                       |        progress = 0
                       |        tasks = [
                       |        ]
                       |        status = {
                       |          state = 0
                       |          time = 0
                       |        }
                       |        history = [
                       |        ]
                       |      }
                       |    }
                       |  }
                       |}
                       |{
                       |  parent = foo
                       |  sequence = 2
                       |  time = 123456789012
                       |  payload = {
                       |    job_completed = {
                       |      outputs = [
                       |      ]
                       |      message = completed
                       |    }
                       |  }
                       |}""".stripMargin
  }
}
