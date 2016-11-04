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

package fr.cnrs.liris.accio.cli

import fr.cnrs.liris.accio.core.framework.{Artifact, Run}

/**
 * Aggregate data about several runs.
 *
 * @param runs Non-empty list of runs to aggregate.
 */
class ReportStatistics(val runs: Seq[Run]) {
  require(runs.nonEmpty, "You must provide some runs to aggregate, got none.")

  /**
   * Return all artifacts as a map with names as keys and artifacts keyed by run id as value.
   */
  def artifacts: Map[String, Map[String, Artifact]] = {
    // First group all artifacts by run id.
    val artifactsByRun = runs.flatMap { run =>
      run.report
        .map(_.artifacts.map(art => run.id -> art))
        .getOrElse(Seq.empty[(String, Artifact)])
    }
    // Then group artifacts by their name.
    artifactsByRun
      .groupBy(_._2.name)
      .map { case (id, group) => id -> group.toMap }
  }
}