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

package fr.cnrs.liris.accio.core.framework

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.util.Seqs
import org.joda.time.DateTime

import scala.util.Random

/**
 * Exception thrown if a run is incorrectly defined.
 *
 * @param message Error message.
 * @param cause   Root cause.
 */
class IllegalRunException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Factory for [[Run]]s.
 *
 * @param opRegistry         Operator registry.
 * @param workflowRepository Workflow Repository.
 */
@Singleton
final class RunFactory @Inject()(parser: RunParser, opRegistry: OpRegistry, workflowRepository: WorkflowRepository) {
  /**
   * Create one or many runs from a definition.
   *
   * @param defn Run definition.
   * @param more More run definitions to merge with the previous one, the later taking precedence.
   * @throws IllegalRunException If the run definition is invalid.
   */
  @throws[IllegalRunException]
  def create(defn: RunDef, more: RunDef*): Seq[Run] = {
    val defns = Seq(defn) ++ more

    // Extract the workflow and package.
    val pkgStr = defns.flatMap(_.pkg).lastOption match {
      case None => throw new IllegalRunException("At least one run definition must define a package")
      case Some(s) => s
    }
    val workflow = getWorkflow(pkgStr)
    val pkg = Package(workflow.id, workflow.version)

    // Check that workflow parameters referenced actually exist.
    val baseParams = defns.flatMap(_.params).toMap
    val unknownParams = baseParams.keySet.diff(workflow.params.map(_.name))
    if (unknownParams.nonEmpty) {
      throw new IllegalRunException(s"Unknown parameters: ${unknownParams.mkString(", ")}")
    }

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(baseParams.keySet)
    if (missingParams.nonEmpty) {
      throw new IllegalRunException(s"Non-optional parameters are unspecified: ${missingParams.mkString(", ")}")
    }

    // Merge properties from all definitions. The last property defined wins, except for tags that are all merged.
    val baseName = defns.flatMap(_.name).lastOption.getOrElse(workflow.id.value)
    val baseSeed = defns.flatMap(_.seed).lastOption.getOrElse(Random.nextLong)
    val repeat = defns.flatMap(_.repeat).lastOption.getOrElse(1)
    val cluster = defns.flatMap(_.cluster).lastOption.getOrElse(Run.DefaultCluster)
    val user = defns.flatMap(_.owner).lastOption.getOrElse(User.Default)
    val environment = defns.flatMap(_.environment).lastOption.getOrElse(Run.DefaultEnvironment)
    val notes = defns.flatMap(_.notes).lastOption
    val tags = defns.flatMap(_.tags).toSet

    // Expand the single run definition into (possibly) many runs.
    if (repeat <= 0) {
      throw new IllegalRunException(s"Number of repetitions must be >= 1, got: $repeat")
    }
    val namesWithParametersAndSeeds =
      expandForParams(defn.params, baseName, workflow)
        .flatMap { case (name, params) => expandForRepeat(repeat, baseSeed, name, params) }

    if (defn.params.isEmpty) {
      namesWithParametersAndSeeds.map { case (name, params, seed) =>
        createSingleRun(pkg, cluster, user, environment, name, notes, tags, seed, params)
      }
    } else {
      val parentId = RunId.random
      val children = namesWithParametersAndSeeds.map { case (name, params, seed) =>
        createChildRun(parentId, pkg, cluster, user, environment, name, seed, params)
      }
      val parent = createParentRun(parentId, children.map(_.id).toSet, pkg, cluster, user, environment, baseName, notes, tags, baseSeed)
      Seq(parent) ++ children
    }
  }

  /**
   * Return the workflow specified by a package definition.
   *
   * @param pkgStr Package definition.
   */
  private def getWorkflow(pkgStr: String) = {
    val maybeWorkflow = pkgStr.split(":") match {
      case Array(id) => workflowRepository.get(WorkflowId(id))
      case Array(id, version) => workflowRepository.get(WorkflowId(id), version.toInt)
      case _ => throw new IllegalRunException(s"Invalid workflow: $pkgStr")
    }
    maybeWorkflow match {
      case None => throw new IllegalRunException(s"Unknown workflow: $pkgStr")
      case Some(workflow) => workflow
    }
  }

  /**
   * Expand an experiment to take into account the space of parameters being explored.
   *
   * @param params   Experiment parameters.
   * @param name     Experiment name.
   * @param workflow Workflow.
   * @return List of (run name, run parameters).
   */
  private def expandForParams(params: Map[String, Exploration], name: String, workflow: Workflow): Seq[(String, Map[String, Any])] = {
    if (params.nonEmpty) {
      val allValues = params.map { case (paramName, explo) =>
        // We are guaranteed that the param exists, because of the experiment construction.
        val param = workflow.params.find(_.name == paramName).get
        explo.expand(param.kind).map(v => (paramName, v)).toSeq
      }.toSeq
      Seqs.crossProduct(allValues).map { params =>
        val label = Run.label(params)
        (s"$name:$label", params.toMap)
      }
    } else {
      Seq((name, Map.empty[String, Any]))
    }
  }

  /**
   * Expand runs to take into account number of times the experiment should be repeated.
   *
   * @param repeat Number of times to repeat the experiment.
   * @param seed   Experiment's seed.
   * @param name   Run name.
   * @param params Run parameters.
   * @return List of (run name, run parameters, run seed).
   */
  private def expandForRepeat(repeat: Int, seed: Long, name: String, params: Map[String, Any]): Seq[(String, Map[String, Any], Long)] = {
    if (repeat <= 1) {
      Seq((name, params, seed))
    } else {
      val rnd = new Random(seed)
      Seq.tabulate(repeat)(idx => (s"$name#${idx + 1}", params, rnd.nextLong()))
    }
  }

  private def createSingleRun(pkg: Package, cluster: String, user: User, environment: String, name: String, notes: Option[String], tags: Set[String], seed: Long, params: Map[String, Any]) = {
    Run(
      id = RunId.random,
      pkg = pkg,
      cluster = cluster,
      owner = user,
      environment = environment,
      name = name,
      notes = notes,
      tags = tags,
      seed = seed,
      params = params,
      parent = None,
      children = Set.empty,
      clonedFrom = None,
      createdAt = DateTime.now,
      status = RunStatus.empty)
  }

  private def createParentRun(id: RunId, children: Set[RunId], pkg: Package, cluster: String, user: User, environment: String, name: String, notes: Option[String], tags: Set[String], seed: Long) = {
    Run(
      id = id,
      pkg = pkg,
      cluster = cluster,
      owner = user,
      environment = environment,
      name = name,
      notes = notes,
      tags = tags,
      seed = seed,
      params = Map.empty,
      parent = None,
      children = children,
      clonedFrom = None,
      createdAt = DateTime.now,
      status = RunStatus.empty)
  }

  private def createChildRun(parent: RunId, pkg: Package, cluster: String, user: User, environment: String, name: String, seed: Long, params: Map[String, Any]) = {
    Run(
      id = RunId.random,
      pkg = pkg,
      cluster = cluster,
      owner = user,
      environment = environment,
      name = name,
      notes = None,
      tags = Set.empty,
      seed = seed,
      params = params,
      parent = Some(parent),
      children = Set.empty,
      clonedFrom = None,
      createdAt = DateTime.now,
      status = RunStatus.empty)
  }

  /*private def parseParams(uri: String, params: Map[String, String], workflow: Workflow) = {
    params.map { case (paramName, value) =>
      val maybeParam = workflow.params.find(_.name == paramName)
      maybeParam match {
        case None => throw new IllegalExperimentException(uri, s"Unknown param: $paramName")
        case Some(param) =>
          val parsedValue = try {
            Values.parse(value, param.kind)
          } catch {
            case e: IllegalArgumentException =>
              throw new IllegalExperimentException(uri, s"Invalid value for param $paramName: $value", e)
          }
          paramName -> SingletonExploration(parsedValue)
      }
    }
  }*/
}