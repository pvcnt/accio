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

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import org.joda.time.{DateTime, Duration}

@JsonIgnoreProperties(ignoreUnknown = true)
case class ExperimentReport(
  startedAt: DateTime = DateTime.now,
  completedAt: Option[DateTime] = None,
  runs: Seq[String] = Seq.empty) {

  /**
   * Return whether the execution is completed, either successfully or not.
   */
  @JsonProperty
  def completed: Boolean = completedAt.nonEmpty

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Option[Duration] = completedAt.map(end => Duration.millis(end.getMillis - startedAt.getMillis))

  /**
   * Return a copy of this report with a new child node.
   *
   * @param id Run identifier
   */
  @throws[IllegalStateException]
  def addRun(id: String): ExperimentReport = copy(runs = runs ++ Seq(id))

  /**
   * Return a copy of this report with the execution marked as completed.
   *
   * @param at Time at which the execution completed
   */
  def complete(at: DateTime = DateTime.now): ExperimentReport = copy(completedAt = Some(at))
}