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

import java.nio.file.Paths
import java.util.UUID

import com.google.inject.Inject
import fr.cnrs.liris.common.random.RandomUtils
import fr.cnrs.liris.common.util.{FileUtils, HashUtils}

/**
 * Exception thrown if an experiment is incorrectly defined.
 *
 * @param uri     URI to the experiment definition.
 * @param message Error message.
 * @param cause   Root cause.
 */
class IllegalExperimentException(uri: String, message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Arguments used to specify or override properties when creating experiments.
 *
 * @param owner  Experiment owner.
 * @param name   Experiment name.
 * @param notes  Some notes.
 * @param tags   Some additional tags.
 * @param repeat Number of times each run will be repeated.
 * @param seed   Seed used by unstable operators.
 * @param params Additional parameter values, no yet parsed.
 */
case class ExperimentArgs(
  owner: Option[User] = None,
  name: Option[String] = None,
  notes: Option[String] = None,
  tags: Set[String] = Set.empty,
  repeat: Option[Int] = None,
  seed: Option[Long] = None,
  params: Map[String, String] = Map.empty)

/**
 * Factory for [[Experiment]]s.
 */
final class ExperimentFactory @Inject()(parser: ExperimentParser, workflowFactory: WorkflowFactory) {
  /**
   * Create an experiment.
   *
   * @param uri  URI to an experiment or workflow definition.
   * @param args Experiment arguments.
   * @throws IllegalExperimentException If the experiment definition is invalid.
   * @throws IllegalWorkflowException   If the workflow definition is invalid.
   */
  @throws[IllegalExperimentException]
  @throws[IllegalWorkflowException]
  def create(uri: String, args: ExperimentArgs): Experiment = {
    if (parser.canRead(uri)) {
      createFromExperiment(uri, args)
    } else {
      createFromWorkflow(uri, args)
    }
  }

  /**
   * Create an experiment from an experiment definition. The experiment definition contains a link to the workflow
   * definition. Properties can be overriden by experiment arguments, that take precedence.
   *
   * @param uri  URI to an experiment definition.
   * @param args Experiment arguments.
   * @throws IllegalExperimentException If the experiment definition is invalid.
   * @throws IllegalWorkflowException   If the workflow definition is invalid.
   */
  private def createFromExperiment(uri: String, args: ExperimentArgs) = {
    val user = args.owner.getOrElse(User.Default)
    val defn = parser.parse(uri)

    // Parse and create the workflow from the uri specified in the workflow definition.
    val workflowUri = if (defn.workflow.startsWith("./")) {
      val path = Paths.get(FileUtils.replaceHome(uri))
      path.resolveSibling(defn.workflow.substring(2)).toString
    } else {
      defn.workflow
    }
    val workflow = workflowFactory.create(workflowUri, user)

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(defn.params.keySet)
    if (missingParams.nonEmpty) {
      throw new IllegalExperimentException(uri, s"Non-optional parameters are unspecified: ${missingParams.mkString(", ")}")
    }

    val name = args.name.orElse(defn.name).getOrElse(getDefaultName(uri))
    val repeat = math.max(1, args.repeat.getOrElse(defn.repeat))
    val seed = args.seed.orElse(defn.seed).getOrElse(RandomUtils.random.nextLong())
    val notes = args.notes.orElse(defn.notes)
    // Argument tags are added to definition tags.
    val tags = defn.tags ++ args.tags
    // Argument parameters take precedence over definition parameters.
    val params = defn.params ++ parseParams(uri, args.params, workflow)

    Experiment(
      id = generateId(),
      name = name,
      workflow = workflow,
      owner = user,
      repeat = repeat,
      notes = notes,
      tags = tags,
      seed = seed,
      params = params)
  }

  /**
   * Create an experiment directly from a workflow definition and experiment arguments.
   *
   * @param uri  URI to a workflow definition.
   * @param args Experiment arguments.
   * @throws IllegalExperimentException If the experiment definition is invalid.
   * @throws IllegalWorkflowException   If the workflow definition is invalid.
   */
  private def createFromWorkflow(uri: String, args: ExperimentArgs) = {
    val user = args.owner.getOrElse(User.Default)

    // Parse and create the workflow from its uri.
    val workflow = workflowFactory.create(uri, user)

    // Check that all non-optional workflow parameters are defined.
    val missingParams = workflow.params.filterNot(_.isOptional).map(_.name).diff(args.params.keySet)
    if (missingParams.nonEmpty) {
      throw new IllegalExperimentException(uri, s"Non-optional parameters are unspecified: ${missingParams.mkString(", ")}")
    }

    val name = args.name.getOrElse(getDefaultName(uri))
    val repeat = args.repeat.map(math.max(1, _)).getOrElse(1)
    val seed = args.seed.getOrElse(RandomUtils.random.nextLong())

    Experiment(
      id = generateId(),
      name = name,
      workflow = workflow,
      owner = user,
      repeat = repeat,
      notes = args.notes,
      tags = args.tags,
      seed = seed,
      params = parseParams(uri, args.params, workflow))
  }

  /**
   * Generate a random identifier for each experiment.
   */
  private def generateId() = HashUtils.sha1(UUID.randomUUID().toString)

  /**
   * Return the default name for an experiment, which is inferred from the filename portion in its uri.
   *
   * @param uri URI to an experiment or workflow definition.
   */
  private def getDefaultName(uri: String) =
    FileUtils.removeExtension(Paths.get(FileUtils.replaceHome(uri)).getFileName.toString)

  /**
   * Parse and validate parameters for a given workflow.
   *
   * @param uri     URI to the experiment definition.
   * @param params   Parameters map (with parameter values as unparsed strings).
   * @param workflow Workflow.
   * @throws IllegalExperimentException If the parameter name is unknown or its value is invalid.
   */
  @throws[IllegalExperimentException]
  private def parseParams(uri: String, params: Map[String, String], workflow: Workflow) = {
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
  }
}