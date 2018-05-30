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

import java.io.ByteArrayInputStream
import java.nio.file.Files

import com.twitter.util.Await
import fr.cnrs.liris.lumos.domain.{Event, Job, thrift}
import fr.cnrs.liris.testing.{CreateTmpDirectory, UnitSpec}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer
import org.joda.time.Instant

import scala.collection.mutable

/**
 * Unit tests for [[BinaryFileEventTransport]].
 */
class BinaryFileEventTransportSpec extends UnitSpec with CreateTmpDirectory {
  behavior of "BinaryFileEventTransport"

  it should "write events" in {
    val file = tmpDir.resolve("events.thrift")
    val transport = new BinaryFileEventTransport(file)
    val time = 123456789012L
    transport.sendEvent(Event("foo", 0, new Instant(time), Event.JobEnqueued(Job(name = "foo"))))
    transport.sendEvent(Event("foo", 1, new Instant(time), Event.JobStarted(message = Some("started"))))
    transport.sendEvent(Event("bar", 0, new Instant(time), Event.JobEnqueued(Job(name = "bar"))))
    transport.sendEvent(Event("foo", 2, new Instant(time), Event.JobCompleted(message = Some("completed"))))
    Await.result(transport.close())

    val events = mutable.ListBuffer.empty[thrift.Event]
    val is = new ByteArrayInputStream(Files.readAllBytes(file))
    events += BinaryScroogeSerializer.read(is, thrift.Event)
    events += BinaryScroogeSerializer.read(is, thrift.Event)
    events += BinaryScroogeSerializer.read(is, thrift.Event)
    events += BinaryScroogeSerializer.read(is, thrift.Event)
    is.available() shouldBe 0

    events should contain theSameElementsInOrderAs Seq(
      thrift.Event("foo", 0, time, thrift.EventPayload.JobEnqueued(thrift.JobEnqueuedEvent(thrift.Job(name = "foo", status = Some(thrift.ExecStatus(thrift.ExecState.Pending, 0, None)))))),
      thrift.Event("foo", 1, time, thrift.EventPayload.JobStarted(thrift.JobStartedEvent(message = Some("started")))),
      thrift.Event("bar", 0, time, thrift.EventPayload.JobEnqueued(thrift.JobEnqueuedEvent(thrift.Job(name = "bar", status = Some(thrift.ExecStatus(thrift.ExecState.Pending, 0, None)))))),
      thrift.Event("foo", 2, time, thrift.EventPayload.JobCompleted(thrift.JobCompletedEvent(message = Some("completed")))))
  }
}
