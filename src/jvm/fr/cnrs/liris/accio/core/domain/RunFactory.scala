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

package fr.cnrs.liris.accio.core.domain

import java.util.UUID

import fr.cnrs.liris.common.util.{HashUtils, Seqs}

import scala.util.Random

/**
 * Factory for [[Run]].
 */
final class RunFactory(workflowRepository: WorkflowRepository) {
  /**
   * Create one or several runs from a run definition.
   *
   * Run templates can trigger a parameter sweep if one the following conditions is met:
   * (1) `repeat` field is set to a value greater than 1 OR
   * (2) `params` field contains at least one parameter with more than one value to take.
   * When a parameter sweep is launched, several runs will be returned, the first one being the parent run, which will
   * not be actually executed, and the other the children, which will be executed in parallel.
   *
   * @param defn Run definition.
   * @param user User creating the runs.
   * @throws InvalidRunDefException
   * @return List of runs.
   */
  @throws[InvalidRunDefException]
  def create(defn: RunDef, user: User): Seq[Run] = {
    // Extract the workflow.
    val workflow = getWorkflow(defn.pkg)

    // Check that workflow parameters referenced actually exist.
    val unknownParams = defn.params.keySet.diff(workflow.params.map(_.name))
    if (unknownParams.nonEmpty) {
      throw new InvalidRunDefException(s"Unknown parameters: ${unknownParams.mkString(", ")}")
    }

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(defn.params.keySet)
    if (missingParams.nonEmpty) {
      throw new InvalidRunDefException(s"Non-optional parameters are unspecified: ${missingParams.mkString(", ")}")
    }

    val defaultParams = workflow.params.filterNot(p => defn.params.contains(p.name)).map { argDef =>
      argDef.name -> argDef.defaultValue.get
    }.toMap

    // Check the repeat parameter is correct.
    val repeat = defn.repeat.getOrElse(1)
    if (repeat <= 0) {
      throw new InvalidRunDefException(s"Number of repetitions must be >= 1, got: $repeat")
    }

    // Expand parameters w.r.t. to parameter sweep and repeat.
    val expandedParams = expandForSweep(defn.params.toMap).flatMap(expandForRepeat(repeat, _))

    // Create all runs.
    val owner = defn.owner.getOrElse(user)
    if (expandedParams.size == 1) {
      Seq(createSingle(workflow, owner, defn.name, defn.notes,
        defn.tags.toSet, defn.seed, defaultParams ++ expandedParams.head, defn.clonedFrom))
    } else {
      createSweep(workflow, owner, defn.name, defn.notes, defn.tags.toSet,
        defn.seed, defaultParams, expandedParams, repeat, defn.clonedFrom)
    }
  }

  /**
   * Create a single run (neither a parent or a child). It can however be a cloned run.
   *
   * @param workflow   Workflow that will be executed.
   * @param owner      User initiating the run.
   * @param name       Human-readable name.
   * @param notes      Notes describing the purpose of the run.
   * @param tags       Arbitrary tags used when looking for runs.
   * @param seed       Seed used by unstable operators.
   * @param params     Values of workflow parameters.
   * @param clonedFrom Identifier of the run this instance has been cloned from.
   */
  private def createSingle(
    workflow: Workflow,
    owner: User,
    name: Option[String] = None,
    notes: Option[String] = None,
    tags: Set[String] = Set.empty,
    seed: Option[Long] = None,
    params: Map[String, Value] = Map.empty,
    clonedFrom: Option[RunId] = None): Run = {

    Run(
      id = randomId,
      pkg = Package(workflow.id, workflow.version),
      owner = owner,
      name = name,
      notes = notes,
      tags = tags,
      seed = seed.getOrElse(Random.nextLong()),
      params = params,
      parent = None,
      children = None,
      clonedFrom = clonedFrom,
      createdAt = System.currentTimeMillis(),
      state = initialState(workflow.graph))
  }

  /**
   * Create several runs representing a parameter sweep.
   *
   * @param workflow       Workflow that will be executed.
   * @param owner          User initiating the run.
   * @param name           Human-readable name.
   * @param notes          Notes describing the purpose of the run.
   * @param tags           Arbitrary tags used when looking for runs.
   * @param seed           Seed used by unstable operators.
   * @param defaultParams  Default values for workflow parameters.
   * @param expandedParams List of values of workflow parameters.
   * @param repeat         Number of times to repeat each run.
   * @param clonedFrom     Identifier of the run this instance has been cloned from.
   * @return
   */
  private def createSweep(
    workflow: Workflow,
    owner: User,
    name: Option[String],
    notes: Option[String],
    tags: Set[String],
    seed: Option[Long],
    defaultParams: Map[String, Value],
    expandedParams: Seq[Map[String, Value]],
    repeat: Int,
    clonedFrom: Option[RunId]): Seq[Run] = {

    val pkg = Package(workflow.id, workflow.version)
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
        owner = owner,
        seed = random.nextLong(),
        params = defaultParams ++ params,
        parent = Some(parentId),
        createdAt = now,
        state = initialState(workflow.graph))
    }
    val parent = Run(
      id = parentId,
      pkg = pkg,
      owner = owner,
      name = name,
      notes = notes,
      tags = tags,
      seed = actualSeed,
      params = Map.empty,
      children = Some(children.map(_.id).toSet),
      clonedFrom = clonedFrom,
      createdAt = now,
      state = initialState(workflow.graph))
    Seq(parent) ++ children
  }

  /**
   * Return a random and unique run identifier.
   */
  private def randomId = RunId(UUID.randomUUID().toString)

  /**
   * Return initial run state.
   *
   * @param graph Graph for which to create state.
   */
  private def initialState(graph: GraphDef) = {
    val nodes = graph.nodes.map { node =>
      NodeState(nodeName = node.name, status = NodeStatus.Waiting)
    }
    RunState(progress = 0, status = RunStatus.Scheduled, nodes = nodes)
  }

  /**
   * Return the workflow specified by a package.
   *
   * @param pkg Package.
   */
  private def getWorkflow(pkg: Package) = {
    val maybeWorkflow = workflowRepository.get(pkg.workflowId, pkg.workflowVersion)
    maybeWorkflow match {
      case None => throw new InvalidRunDefException(s"Unknown workflow: $pkg")
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