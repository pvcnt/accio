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

package fr.cnrs.liris.lumos.state

import fr.cnrs.liris.lumos.domain._
import fr.cnrs.liris.testing.UnitSpec
import org.joda.time.Instant

/**
 * Unit tests for [[JobStateMachine]].
 */
class JobStateMachineSpec extends UnitSpec {
  behavior of "JobStateMachine"

  it should "handle JobEnqueued events" in {
    val now = Instant.now()
    val job = Job(
      name = "foo",
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo"),
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1")))
    val event = Event("foo", 0, now, Event.JobEnqueued(job))
    val res = JobStateMachine.apply(Job(), event)

    res shouldBe Right(Job(
      name = "foo",
      createTime = now, // Defined.
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo"),
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1", status = ExecStatus(ExecStatus.Pending, now, message = Some("Job created")))),
      status = ExecStatus(ExecStatus.Pending, now, Some("Job created")), // Defined.
      history = Seq.empty /* Defined */))
  }

  it should "handle JobScheduled events" in {
    val now = Instant.now()
    val job = Job(
      name = "foo",
      createTime = now,
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo"),
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1")),
      status = ExecStatus(ExecStatus.Pending, now, Some("Job created")))
    val event = Event("foo", 0, now, Event.JobScheduled(Map("a" -> "b"), Some("barbar")))
    val res = JobStateMachine.apply(job, event)

    res shouldBe Right(Job(
      name = "foo",
      createTime = now,
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo", "a" -> "b"), // Redefined.
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1")),
      status = ExecStatus(ExecStatus.Scheduled, now, Some("barbar")), // Defined.
      history = Seq(ExecStatus(ExecStatus.Pending, now, Some("Job created"))) /* Defined */))
  }

  it should "handle JobStarted events" in {
    val now = Instant.now()
    val job = Job(
      name = "foo",
      createTime = now,
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo"),
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1")),
      status = ExecStatus(ExecStatus.Pending, now, Some("Job created")))
    val event = Event("foo", 0, now, Event.JobStarted(Some("barbar")))
    val res = JobStateMachine.apply(job, event)

    res shouldBe Right(Job(
      name = "foo",
      createTime = now,
      owner = Some("me"),
      contact = Some("me@ucl.ac.uk"),
      labels = Map("foo" -> "bar"),
      metadata = Map("bar" -> "foo"),
      inputs = Seq(AttrValue("p1", Value.Int(42))),
      tasks = Seq(Task("task1")),
      status = ExecStatus(ExecStatus.Running, now, Some("barbar")), // Defined.
      history = Seq(ExecStatus(ExecStatus.Pending, now, Some("Job created"))) /* Defined */))
  }
}