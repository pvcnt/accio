/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.dsl

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty, JsonSubTypes}
import fr.cnrs.liris.accio.api.thrift

private[dsl] case class ExperimentDsl(
  workflow: String,
  name: Option[String],
  notes: Option[String],
  tags: Seq[String] = Seq.empty,
  seed: Option[Long],
  params: Map[String, ExplorationDsl] = Map.empty,
  repeat: Option[Int])

private[dsl] case class ExplorationDsl(values: Seq[thrift.Value])

private[dsl] case class WorkflowDsl(
  graph: Seq[NodeDsl],
  id: Option[String],
  owner: Option[String],
  name: Option[String],
  params: Seq[ArgDefDsl] = Seq.empty)

private[dsl] case class ArgDefDsl(
  name: String,
  kind: String,
  defaultValue: Option[thrift.Value],
  help: Option[String])

private[dsl] case class NodeDsl(
  op: String,
  @JsonProperty("name")
  customName: Option[String],
  inputs: Map[String, InputDsl] = Map.empty) {

  def name: String = customName.getOrElse(op)
}

@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[InputDsl.Value], name = "value"),
  new JsonSubTypes.Type(value = classOf[InputDsl.Reference], name = "reference"),
  new JsonSubTypes.Type(value = classOf[InputDsl.Param], name = "param")))
@JsonIgnoreProperties(ignoreUnknown = true)
private[dsl] sealed trait InputDsl

object InputDsl {

  private[dsl] case class Value(value: thrift.Value) extends InputDsl

  private[dsl] case class Reference(reference: String) extends InputDsl

  private[dsl] case class Param(param: String) extends InputDsl

}