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

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Definition of one or several runs. The difference with the canonical [[Run]] structure is that this definition
 * allows to specify multiple runs at once, thanks to the `repeat` property, which specifies the number of times the
 * run should be repeated, and the `params` property, which specifies exploration spaces for parameters instead of
 * single values.
 *
 * You should use a [[RunFactory]] to convert a [[RunDef]] into [[Run]]s.
 *
 * @param pkg         Description of the workflow.
 * @param cluster     Cluster on which the run is executed.
 * @param owner       User that initiated the run.
 * @param environment Environment inside which the run is executed.
 * @param name        Human-readable name.
 * @param notes       Notes describing the purpose of the run.
 * @param tags        Arbitrary tags helping with run classification.
 * @param seed        Seed used by unstable operators.
 * @param repeat      Number of times to repeat each run.
 * @param params      Values of workflow parameters.
 */
case class RunDef(
  @JsonProperty("package") pkg: Option[String] = None,
  cluster: Option[String] = None,
  owner: Option[User] = None,
  environment: Option[String] = None,
  name: Option[String] = None,
  notes: Option[String] = None,
  tags: Set[String] = Set.empty,
  seed: Option[Long] = None,
  repeat: Option[Int] = None,
  params: Map[String, Exploration] = Map.empty)