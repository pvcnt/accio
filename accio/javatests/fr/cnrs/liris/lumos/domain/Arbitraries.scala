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

package fr.cnrs.liris.lumos.domain

import org.scalacheck.Arbitrary

object Arbitraries {
  implicit def execState: Arbitrary[ExecStatus.State] = Arbitrary(Generators.execState)

  implicit def remoteFile: Arbitrary[RemoteFile] = Arbitrary(Generators.remoteFile)

  implicit def attrValue: Arbitrary[AttrValue] = Arbitrary(Generators.attrValue)

  implicit def metricValue: Arbitrary[MetricValue] = Arbitrary(Generators.metricValue)

  implicit def errorDatum: Arbitrary[ErrorDatum] = Arbitrary(Generators.errorDatum)

  implicit def dataType: Arbitrary[DataType] = Arbitrary(Generators.dataType)

  implicit def value: Arbitrary[Value] = Arbitrary(Generators.value)

  implicit def job: Arbitrary[Job] = Arbitrary(Generators.job)

  implicit def event: Arbitrary[Event] = Arbitrary(Generators.event)
}
