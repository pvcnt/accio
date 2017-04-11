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

package fr.cnrs.liris.accio.core.framework

import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api.Utils
import fr.cnrs.liris.accio.core.api.thrift._
import fr.cnrs.liris.accio.core.storage.Storage
import fr.cnrs.liris.common.util.Seqs
import fr.cnrs.liris.dal.core.api.Value

import scala.collection.mutable
import scala.util.Random

/**
 * Factory for [[Run]].
 *
 * @param storage        Storage.
 * @param valueValidator Value validator.
 */
final class RunFactory @Inject()(storage: Storage, valueValidator: ValueValidator) extends BaseFactory {
  /**
   * Create one or several runs from a run specification.
   *
   * Run templates can trigger a parameter sweep if one the following conditions is met:
   * (1) `repeat` field is set to a value greater than 1 OR
   * (2) `params` field contains at least one parameter with more than one value to take.
   * When a parameter sweep is launched, several runs will be returned, the first one being the parent run, which will
   * not be actually executed, and the other the children, which will be executed in parallel.
   *
   * @param spec     Run specification.
   * @param user     User creating the runs.
   * @param warnings Mutable list collecting warnings.
   * @throws InvalidSpecException If the run specification is invalid.
   * @return List of runs.
   */
  @throws[InvalidSpecException]
  def create(spec: RunSpec, user: User, warnings: mutable.Set[InvalidSpecMessage] = mutable.Set.empty[InvalidSpecMessage]): Seq[Run] = {
    // Extract the workflow.
    val workflow = getWorkflow(spec.pkg, warnings)

    // Check that workflow parameters referenced actually exist.
    val unknownParams = spec.params.keySet.diff(workflow.params.map(_.name))
    if (unknownParams.nonEmpty) {
      throw newError("Unknown", unknownParams.map(paramName => s"params.$paramName"), warnings)
    }

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(spec.params.keySet)
    if (missingParams.nonEmpty) {
      throw newError("Required param is missing", missingParams.map(paramName => s"params.$paramName"), warnings)
    }

    val defaultParams = workflow.params.filterNot(p => spec.params.contains(p.name)).flatMap { argDef =>
      argDef.defaultValue.map(defaultValue => argDef.name -> defaultValue)
    }.toMap

    // Check the repeat parameter is correct.
    val repeat = spec.repeat.getOrElse(1)
    if (repeat <= 0) {
      throw newError(s"Number of times must be >= 1", warnings)
    }

    // Expand parameters w.r.t. to parameter sweep and repeat.
    val expandedParams = expandForSweep(spec.params.toMap).flatMap(expandForRepeat(repeat, _))
    val errors = expandedParams.flatten.flatMap { case (name, value) =>
      valueValidator.validate(value, workflow.params.find(_.name == name).get, Some(s"params.$name"))
    }
    if (errors.nonEmpty) {
      throw new InvalidSpecException(errors)
    }

    // Create all runs.
    val owner = spec.owner.getOrElse(user)
    if (expandedParams.size == 1) {
      Seq(createSingle(workflow, owner, spec.name, spec.notes,
        spec.tags.toSet, spec.seed, defaultParams ++ expandedParams.head, spec.clonedFrom))
    } else {
      val fixedParams = spec.params.filter(_._2.size <= 1).keySet
      createSweep(workflow, owner, spec.name, spec.notes, spec.tags.toSet,
        spec.seed, defaultParams, fixedParams.toSet, expandedParams, repeat, spec.clonedFrom)
    }
  }

  /**
   * Validate that a run specification would create valid run(s). It does not throw any exception if the specification
   * is invalid, errors are returned.
   *
   * @param spec Run specification.
   */
  def validate(spec: RunSpec): ValidationResult = {
    doValidate(warnings => create(spec, User("dummy user"), warnings))
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
      pkg = Package(workflow.id, workflow.version.get),
      owner = owner,
      name = name,
      cluster = "default",
      notes = notes,
      tags = tags,
      seed = seed.getOrElse(Random.nextLong()),
      params = params,
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
   * @param fixedParams    Names of parameters whose value does not vary across runs.
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
    fixedParams: Set[String],
    expandedParams: Seq[Map[String, Value]],
    repeat: Int,
    clonedFrom: Option[RunId]): Seq[Run] = {

    val pkg = Package(workflow.id, workflow.version.get)
    val actualSeed = seed.getOrElse(Random.nextLong())
    val now = System.currentTimeMillis()
    val random = new Random(actualSeed)
    val parentId = randomId

    // Impl. note: As of now, with Scala 2.11.8, I have to explicitly write the type of `children` to be Seq[Run].
    // If I don't force it, I get the strangest compiler error (head lines follow):
    // java.lang.AssertionError: assertion failed:
    //   Some(children.<map: error>(((x$2) => x$2.id)).<toSet: error>)
    //     while compiling: /path/to/code/src/jvm/fr/cnrs/liris/accio/core/api/RunFactory.scala
    //       during phase: superaccessors
    val children: Seq[Run] = expandedParams.map { params =>
      val discriminantParams = params.filter { case (key, _) => !fixedParams.contains(key) }
      val maybeName = if (discriminantParams.nonEmpty) Some(Utils.label(discriminantParams)) else None
      Run(
        id = randomId,
        pkg = pkg,
        owner = owner,
        name = maybeName,
        cluster = "default",
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
      cluster = "default",
      name = name,
      notes = notes,
      tags = tags,
      seed = actualSeed,
      params = Map.empty,
      children = children.map(_.id),
      clonedFrom = clonedFrom,
      createdAt = now,
      state = RunState(progress = 0, status = RunStatus.Scheduled))

    Seq(parent) ++ children
  }

  /**
   * Return a random and unique run identifier.
   */
  private def randomId = RunId(UUID.randomUUID().toString.replace("-", ""))

  /**
   * Return initial run state.
   *
   * @param graph Graph for which to create state.
   */
  private def initialState(graph: Graph) = {
    val nodes = graph.nodes.map { node =>
      NodeState(name = node.name, status = NodeStatus.Waiting)
    }
    RunState(progress = 0, status = RunStatus.Scheduled, nodes = nodes)
  }

  /**
   * Return the workflow defined by a package.
   *
   * @param pkg      Package.
   * @param warnings Mutable list collecting warnings.
   */
  private def getWorkflow(pkg: Package, warnings: mutable.Set[InvalidSpecMessage]) = {
    val maybeWorkflow = storage.read(_.workflows.get(pkg.workflowId, pkg.workflowVersion))
    maybeWorkflow match {
      case None => throw newError(s"Workflow not found: ${pkg.workflowId.value}@${pkg.workflowVersion}", "pkg", warnings)
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