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

package fr.cnrs.liris.accio.server

import com.google.inject.{Inject, Singleton}
import com.twitter.inject.annotations.Flag
import com.twitter.util.Future
import fr.cnrs.liris.accio.discovery.DiscoveryModule
import fr.cnrs.liris.accio.domain.thrift.ThriftAdapter
import fr.cnrs.liris.accio.domain.{Graph, Workflow}
import fr.cnrs.liris.accio.scheduler.{Process, Scheduler}
import fr.cnrs.liris.accio.validation.WorkflowFactory
import fr.cnrs.liris.lumos.domain.{Event, Job, Task}
import fr.cnrs.liris.lumos.transport.{EventTransport, EventTransportModule}
import fr.cnrs.liris.util.jvm.JavaHome
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer
import org.joda.time.Instant

import scala.collection.mutable
import scala.util.control.NonFatal

@Singleton
final class SubmitWorkflowService @Inject()(
  factory: WorkflowFactory,
  scheduler: Scheduler,
  transport: EventTransport,
  @Flag("executor_uri") executorUri: String) {

  def apply(workflow: Workflow): Future[Seq[Workflow]] = {
    val workflows = factory.create(workflow)
    val fs = workflows.map { child =>
      val graph = Graph.create(child)
      val tasks = child.steps.map { step =>
        Task(
          name = step.name,
          mnemonic = Some(step.op),
          dependencies = graph(step.name).predecessors)
      }
      val job = Job(
        name = child.name,
        owner = workflow.owner,
        contact = workflow.contact,
        labels = workflow.labels,
        tasks = tasks,
        inputs = workflow.params)

      transport.sendEvent(Event(workflow.name, 0, Instant.now(), Event.JobEnqueued(job)))
      val cmd = mutable.ListBuffer.empty[String]
      cmd += JavaHome.javaBinary.toString
      cmd += s"-Xms200M"
      cmd += s"-Xmx200M"
      cmd ++= Seq("-jar", executorUri)
      cmd ++= EventTransportModule.args
      cmd ++= DiscoveryModule.args
      cmd += BinaryScroogeSerializer.toString(ThriftAdapter.toThrift(workflow))
      val process = Process(child.name, cmd.mkString(" "), workflow.resources)
      scheduler
        .submit(process)
        .onSuccess { info =>
          transport.sendEvent(Event(workflow.name, 1, Instant.now(), Event.JobScheduled(info.metadata)))
        }
        .onFailure { case NonFatal(e) =>
          transport.sendEvent(Event(workflow.name, 1, Instant.now(), Event.JobCompleted(message = Some(s"Failed to schedule job: ${e.getMessage}"))))
        }
    }
    Future.collect(fs).map(_ => workflows)
  }
}