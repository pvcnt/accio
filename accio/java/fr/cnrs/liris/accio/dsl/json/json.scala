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

package fr.cnrs.liris.accio.dsl.json

import fr.cnrs.liris.accio.api.thrift

private[json] case class JobDsl(
  name: Option[String],
  title: Option[String],
  tags: Seq[String] = Seq.empty,
  seed: Option[Long],
  params: Seq[NamedValueDsl] = Seq.empty,
  steps: Seq[StepDsl] = Seq.empty)

private[json] case class NamedValueDsl(name: String, value: thrift.Value)

private[json] case class StepDsl(
  op: String,
  name: Option[String],
  inputs: Seq[NameChannelDsl] = Seq.empty,
  exports: Seq[ExportDsl] = Seq.empty)

private[json] case class NameChannelDsl(name: String, channel: thrift.Channel)

private[json] case class ExportDsl(output: String, exportAs: String)