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
import fr.cnrs.liris.common.util.{FileUtils, HashUtils}

/**
 * Exception thrown if an experiment is incorrectly defined.
 *
 * @param message Error message
 * @param cause   Root cause
 */
class IllegalExperimentException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Parser for [[ExperimentDef]]s.
 */
trait ExperimentParser {
  /**
   * Parse a file into an experiment definition.
   *
   * @param uri URI to an experiment definition.
   */
  def parse(uri: String): ExperimentDef

  /**
   * Check whether a given URI could be read as an experiment definition.
   *
   * @param uri URI to (possibly) an experiment definition.
   * @return True if it could be read as an experiment definition, false otherwise
   */
  def canRead(uri: String): Boolean
}

/**
 * Factory for [[Experiment]]s.
 */
final class ExperimentFactory @Inject()(parser: ExperimentParser, workflowFactory: WorkflowFactory) {
  /**
   * Create an experiment.
   *
   * @param uri  URI to an experiment definition.
   * @param user User creating this experiment.
   * @throws IllegalExperimentException If the experiment definition is invalid.
   */
  @throws[IllegalExperimentException]
  def create(uri: String, user: User): Experiment = {
    val path = Paths.get(FileUtils.replaceHome(uri))
    // Generate a random identifier for each experiment.
    val id = HashUtils.sha1(UUID.randomUUID().toString)

    if (parser.canRead(uri)) {
      val defn = parser.parse(uri)

      // Experiment contains URI to workflow, get and parse its content.
      val workflowUri = if (defn.workflow.startsWith("./")) {
        path.resolveSibling(defn.workflow.substring(2)).toString
      } else {
        defn.workflow
      }
      val workflow = workflowFactory.create(workflowUri, user)

      // Use name provided in the definition, or filename (without extension) otherwise.
      val name = defn.name.getOrElse(FileUtils.removeExtension(path.getFileName.toString))
      // Use owner provided in the definition, or current user otherwise.
      val owner = defn.owner.getOrElse(user)

      Experiment(
        id = id,
        name = name,
        workflow = workflow,
        owner = owner,
        runs = math.max(1, defn.runs),
        notes = defn.notes,
        tags = defn.tags,
        params = defn.params)
    } else {
      val workflow = workflowFactory.create(uri, user)
      // Use filename (without extension) as name.
      val name = FileUtils.removeExtension(path.getFileName.toString)

      Experiment(
        id = id,
        name = name,
        workflow = workflow,
        owner = user,
        runs = 1,
        notes = None,
        tags = Set.empty,
        params = Map.empty)
    }
  }
}