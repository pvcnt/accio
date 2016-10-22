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

import com.twitter.finatra.validation.Min

/**
 * An experiment is a specification about the manner to run one or many variations of a single workflow. It can be
 * either a direct execution of this workflow, an exploration of parameters or an optimization of parameters.
 *
 * @param id          Unique identifier (among all experiments AND runs).
 * @param name        Human-readable name.
 * @param workflow    Base workflow.
 * @param owner       User initiating the experiment.
 * @param runs
 * @param notes       Some notes.
 * @param tags        Some tags.
 * @param params      Override parameters.
 * @param exploration Parameters sweep.
 * @param report
 */
case class Experiment(
  id: String,
  name: String,
  workflow: Workflow,
  owner: User,
  runs: Int,
  notes: Option[String],
  tags: Set[String],
  params: Map[Reference, Any],
  exploration: Option[Exploration],
  report: Option[ExperimentReport] = None) {

  def shortId: String = id.substring(0, 8)
}

case class ExperimentDef(
  workflow: String,
  name: Option[String] = None,
  owner: Option[User] = None,
  notes: Option[String] = None,
  tags: Set[String] = Set.empty,
  @Min(1) runs: Int = 1,
  params: Map[Reference, Any] = Map.empty,
  exploration: Option[Exploration] = None)