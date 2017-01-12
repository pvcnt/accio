/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.domain

import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.common.util.Seqs

import scala.util.Random

/**
 * Factory for [[Run]].
 */
final class RunFactory @Inject()(workflowRepository: WorkflowRepository) {
  @throws[InvalidRunException]
  def create(spec: RunSpec): Seq[Run] = {
    // Extract the workflow and package.
    val workflow = getWorkflow(spec.pkg)
    val pkg = Package(workflow.id, workflow.version)

    // Check that workflow parameters referenced actually exist.
    val unknownParams = spec.params.keySet.diff(workflow.params.map(_.name))
    if (unknownParams.nonEmpty) {
      throw new InvalidRunException(s"Unknown parameters: ${unknownParams.mkString(", ")}")
    }

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(spec.params.keySet)
    if (missingParams.nonEmpty) {
      throw new InvalidRunException(s"Non-optional parameters are unspecified: ${missingParams.mkString(", ")}")
    }

    // Check the repeat parameter is correct.
    val repeat = spec.repeat.getOrElse(1)
    if (repeat <= 0) {
      throw new InvalidRunException(s"Number of repetitions must be >= 1, got: $repeat")
    }

    // Expand parameters w.r.t. to parameter sweep and repeat.
    val expandedParams = expandForSweep(spec.params.toMap).flatMap(expandForRepeat(repeat, _))

    // Create all runs.
    if (expandedParams.size == 1) {
      Seq(createSingle(pkg, spec.entitlement, spec.name, spec.notes, spec.tags.toSet, spec.seed, expandedParams.head, spec.clonedFrom))
    } else {
      createSweep(pkg, spec.entitlement, spec.name, spec.notes, spec.tags.toSet, spec.seed, expandedParams, repeat, spec.clonedFrom)
    }
  }

  private def createSingle(
    pkg: Package,
    entitlement: Entitlement,
    name: Option[String] = None,
    notes: Option[String] = None,
    tags: Set[String] = Set.empty,
    seed: Option[Long] = None,
    params: Map[String, Value] = Map.empty,
    clonedFrom: Option[RunId] = None): Run = {

    Run(
      id = randomId,
      pkg = pkg,
      entitlement = entitlement,
      name = name,
      notes = notes,
      tags = tags,
      seed = seed.getOrElse(Random.nextLong()),
      params = params,
      parent = None,
      children = None,
      clonedFrom = clonedFrom,
      createdAt = System.currentTimeMillis(),
      state = initialState)
  }

  private def createSweep(
    pkg: Package,
    entitlement: Entitlement,
    name: Option[String],
    notes: Option[String],
    tags: Set[String],
    seed: Option[Long],
    expandedParams: Seq[Map[String, Value]],
    repeat: Int,
    clonedFrom: Option[RunId]): Seq[Run] = {

    val actualSeed = seed.getOrElse(Random.nextLong())
    val parentId = randomId
    val now = System.currentTimeMillis()
    val random = new Random(actualSeed)
    // Impl. note: As of now, with Scala 2.11.8, I have to explicitly write the type of `children` to be Seq[Run].
    // If I don't force it, I get the strangest compiler error (head lines follow):
    // java.lang.AssertionError: assertion failed:
    //   Some(children.<map: error>(((x$2) => x$2.id)).<toSet: error>)
    //     while compiling: /path/to/code/src/jvm/fr/cnrs/liris/accio/core/domain/RunFactory.scala
    //       during phase: superaccessors
    val children: Seq[Run] = expandedParams.map { params =>
      Run(
        id = randomId,
        pkg = pkg,
        entitlement = entitlement,
        seed = random.nextLong(),
        params = params,
        parent = Some(parentId),
        createdAt = now,
        state = initialState)
    }
    val parent = Run(
      id = parentId,
      pkg = pkg,
      entitlement = entitlement,
      name = name,
      notes = notes,
      tags = tags,
      seed = actualSeed,
      params = Map.empty,
      children = Some(children.map(_.id).toSet),
      clonedFrom = clonedFrom,
      createdAt = now,
      state = initialState)
    Seq(parent) ++ children
  }

  private def randomId = RunId(UUID.randomUUID().toString)

  private def initialState = RunState(progress = 0, status = RunStatus.Scheduled)

  /**
   * Return the workflow specified by a package definition.
   *
   * @param pkgStr Package definition.
   */
  private def getWorkflow(pkgStr: String) = {
    val maybeWorkflow = pkgStr.split(":") match {
      case Array(id) => workflowRepository.get(WorkflowId(id))
      case Array(id, version) => workflowRepository.get(WorkflowId(id), version)
      case _ => throw new InvalidRunException(s"Invalid workflow: $pkgStr")
    }
    maybeWorkflow match {
      case None => throw new InvalidRunException(s"Unknown workflow: $pkgStr")
      case Some(workflow) => workflow
    }
  }

  /**
   * Expand an experiment to take into account the space of parameters being explored.
   *
   * @param paramsSweep Parameters sweep.
   * @return List of run parameters.
   */
  private def expandForSweep(paramsSweep: Map[String, Seq[Value]]): Seq[Map[String, Value]] = {
    if (paramsSweep.nonEmpty) {
      val allValues = paramsSweep.map { case (paramName, values) =>
        // We are guaranteed that the param exists, because of the experiment construction.
        values.map(v => (paramName, v))
      }.toSeq
      Seqs.crossProduct(allValues).map(_.toMap)
    } else {
      Seq(Map.empty[String, Value])
    }
  }

  /**
   * Expand runs to take into account number of times the experiment should be repeated.
   *
   * @param repeat Number of times to repeat the experiment.
   * @return List of (run name, run parameters, run seed).
   */
  private def expandForRepeat(repeat: Int, params: Map[String, Value]): Seq[Map[String, Value]] = {
    if (repeat <= 1) {
      Seq(params)
    } else {
      Seq.fill(repeat)(params)
    }
  }
}