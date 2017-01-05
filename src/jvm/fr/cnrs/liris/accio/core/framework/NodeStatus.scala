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
import fr.cnrs.liris.common.util.Named
import org.joda.time.{DateTime, Duration}

/**
 * Execution status of a node.
 *
 * @param startedAt   Time at which the execution started
 * @param completedAt Time at which the execution completed
 * @param successful  Did the execution completed successfully?
 * @param stdout
 * @param stderr
 * @param exitCode
 * @param exception
 * @param artifacts
 * @param metrics     Metrics generated during the execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
case class NodeStatus(
  startedAt: DateTime,
  completedAt: DateTime,
  successful: Boolean,
  stdout: String,
  stderr: String,
  exitCode: Int,
  exception: Option[ExceptionData],
  artifacts: Seq[Artifact],
  metrics: Seq[Metric]) {

  /**
   * Return the execution duration.
   */
  @JsonProperty
  def duration: Duration = Duration.millis(completedAt.getMillis - startedAt.getMillis)
}

case class NodeKey(value: String)

case class Metric(name: String, value: Double)

/**
 *
 * @param name
 * @param kind
 * @param value
 */
case class Artifact(name: String, @JsonProperty("type") kind: DataType, value: Any) extends Named