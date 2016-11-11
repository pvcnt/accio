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
 * An experiment is the execution of one or several variations of a single workflow. The actual execution of a
 * workflow takes place inside [[Run]]s, which are then aggregated into an experiment. An experiment can specify
 * some parameters, which allow to specify values for some ports.
 *
 * @param id       Unique identifier (among all experiments AND runs).
 * @param name     Human-readable name.
 * @param workflow Workflow being executed.
 * @param owner    User initiating the experiment.
 * @param repeat   Number of times to repeat each run.
 * @param notes    Some notes.
 * @param tags     Some tags.
 * @param params   Parameters override.
 * @param seed     Seed used by unstable operators.
 * @param report   Execution report.
 */
case class Experiment(
  id: String,
  name: String,
  workflow: Workflow,
  owner: User,
  repeat: Int,
  notes: Option[String],
  tags: Set[String],
  params: Map[String, Exploration],
  seed: Long,
  report: Option[ExperimentReport] = None) {

  def shortId: String = id.substring(0, 8)
}

case class ExperimentDef(
  workflow: String,
  name: Option[String] = None,
  owner: Option[User] = None,
  notes: Option[String] = None,
  tags: Set[String] = Set.empty,
  @Min(1) repeat: Int = 1,
  seed: Option[Long] = None,
  params: Map[String, Exploration] = Map.empty)