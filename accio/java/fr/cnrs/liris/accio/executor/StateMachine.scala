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

package fr.cnrs.liris.accio.executor

import java.util.concurrent.atomic.AtomicLong

import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.domain._
import fr.cnrs.liris.lumos.domain.{AttrValue, Event, ExecStatus}
import org.joda.time.Instant

import scala.collection.mutable

final class StateMachine(workflow: Workflow) extends Logging {
  private[this] val artifacts = mutable.Map.empty[String, AttrValue]
  private[this] val tasks = mutable.Map.empty[String, ExecStatus.State]
  private[this] val graph = Graph.create(workflow)
  // There has already been a JobEnqueued and a JobScheduled event, so we started at 2.
  private[this] val sequence = new AtomicLong(2)

  workflow.steps.foreach(step => tasks(step.name) = ExecStatus.Pending)

  def startJob(): Seq[SideEffect] = synchronized {
    Seq(SideEffect.Publish(createEvent(Event.JobStarted()))) ++ startAllSteps()
  }

  def cancelJob(): Seq[SideEffect] = synchronized {
    cancelAllSteps() ++ Seq(SideEffect.Publish(createEvent(Event.JobCanceled())))
  }

  def currentState: ExecStatus.State = synchronized {
    // We should take care of only returning a completed state if the workflow is effectively
    // completed, i.e., all its steps are completed. For example, we may already know that the
    // workflow will eventually be marked as canceled, but if some steps are still running the best
    // we can return is Running.
    val states = tasks.values.toSet
    if (states.contains(ExecStatus.Running)) {
      ExecStatus.Running
    } else if (states.contains(ExecStatus.Scheduled)) {
      ExecStatus.Scheduled
    } else if (states == Set(ExecStatus.Successful)) {
      ExecStatus.Successful
    } else if (states.forall(_.isCompleted) && states.contains(ExecStatus.Canceled)) {
      ExecStatus.Canceled
    } else if (states.forall(_.isCompleted) && states.contains(ExecStatus.Lost)) {
      ExecStatus.Lost
    } else if (states.forall(_.isCompleted)) {
      ExecStatus.Failed
    } else {
      ExecStatus.Pending
    }
  }

  def stepStarted(stepName: String): Seq[SideEffect] = synchronized {
    val sideEffects = mutable.ListBuffer.empty[SideEffect]
    if (tasks(stepName) != ExecStatus.Pending) {
      logger.warn(s"Attempted to start already started task $stepName")
    } else {
      tasks(stepName) = ExecStatus.Running
      sideEffects += SideEffect.Publish(createEvent(Event.TaskStarted(stepName)))
    }
    sideEffects.toList
  }

  def stepCompleted(stepName: String, exitCode: Int, result: OpResult): Seq[SideEffect] = synchronized {
    val sideEffects = mutable.ListBuffer.empty[SideEffect]
    if (tasks(stepName) != ExecStatus.Running) {
      logger.warn(s"Attempted to complete already completed task $stepName")
    } else {
      if (exitCode == 0) {
        artifacts ++= result.artifacts.map { value =>
          val name = s"$stepName/${value.name}"
          name -> value.copy(name = name)
        }
        tasks(stepName) = ExecStatus.Successful
        sideEffects += SideEffect.Publish(createEvent(Event.TaskCompleted(
          name = stepName,
          exitCode = exitCode,
          metrics = result.metrics)))
        sideEffects ++= scheduleNextSteps(stepName)
      } else {
        tasks(stepName) = ExecStatus.Failed
        sideEffects += SideEffect.Publish(createEvent(Event.TaskCompleted(
          name = stepName,
          exitCode = exitCode,
          metrics = result.metrics,
          error = result.error,
          message = Some("Step completed with non-null exit code"))))
        sideEffects ++= cancelNextSteps(stepName)
      }
    }

    if (currentState.isCompleted) {
      val outputs = artifacts.values.toSeq.sortBy(_.name)
      sideEffects += SideEffect.Publish(createEvent(Event.JobCompleted(outputs)))
    }

    sideEffects.toList
  }

  private def scheduleNextSteps(stepName: String): Seq[SideEffect] = {
    getNextNodes(stepName)
      .filter(node => node.predecessors.forall(dep => tasks(dep) == ExecStatus.Successful))
      .flatMap(node => scheduleStep(node.name))
      .toSeq
  }

  private def cancelNextSteps(stepName: String): Seq[SideEffect] = {
    getNextNodes(stepName)
      .flatMap(node => Seq(node) ++ getNextNodes(node.name))
      .flatMap(node => cancelStep(node.name))
      .toSeq
  }

  private def startAllSteps(): Seq[SideEffect] = {
    graph.roots.flatMap(node => scheduleStep(node.name)).toSeq
  }

  private def cancelAllSteps(): Seq[SideEffect] = {
    tasks.filter { case (_, v) => v != ExecStatus.Pending && !v.isCompleted }
      .keys
      .flatMap(cancelStep)
      .toSeq
  }

  private def scheduleStep(stepName: String): Seq[SideEffect] = {
    Seq(SideEffect.Schedule(stepName, createPayload(stepName)))
  }

  private def cancelStep(stepName: String): Seq[SideEffect] = {
    tasks(stepName) = ExecStatus.Canceled
    Seq(SideEffect.Kill(stepName))
  }

  private def getNextNodes(stepName: String): Set[Graph.Node] = {
    graph(stepName)
      .successors
      .filter(name => tasks(name) == ExecStatus.Pending)
      .map(graph.apply)
  }

  private def createPayload(stepName: String): OpPayload = {
    val step = workflow.steps.find(_.name == stepName).get
    val params = step.params.map { channel =>
      channel.source match {
        case Channel.Constant(v) => AttrValue(channel.name, v)
        case Channel.Param(paramName) =>
          val param = workflow.params.find(_.name == paramName).get
          AttrValue(channel.name, param.value)
        case Channel.Reference(otherName, outputName) =>
          val artifact = artifacts(s"$otherName/$outputName")
          AttrValue(channel.name, artifact.value)
      }
    }
    OpPayload(step.op, workflow.seed, params, workflow.resources)
  }

  private def createEvent(payload: Event.Payload) = {
    Event(workflow.name, sequence.getAndIncrement(), Instant.now, payload)
  }
}
