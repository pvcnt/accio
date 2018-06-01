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

package fr.cnrs.liris.accio.domain

import fr.cnrs.liris.lumos.domain.Generators._
import org.scalacheck.Arbitrary.{arbitrary, _}
import org.scalacheck.Gen

object Generators {
  private val resources: Gen[Map[String, Long]] = Gen.mapOf(alphaNumString.flatMap(k => Gen.posNum[Long].map(v => k -> v)))

  private val attribute: Gen[Attribute] = for {
    name <- alphaNumString
    dataType <- dataType
    help <- Gen.option(alphaNumString)
    defaultValue <- Gen.option(value)
    optional <- arbitrary[Boolean]
    aspects <- Gen.listOf(alphaNumString)
  } yield Attribute(name, dataType, help, defaultValue, optional, aspects.toSet)

  val operator: Gen[Operator] = for {
    name <- alphaNumString
    executable <- remoteFile
    category <- alphaNumString
    help <- Gen.option(alphaNumString)
    description <- Gen.option(alphaNumString)
    inputs <- Gen.listOf(attribute)
    outputs <- Gen.listOf(attribute)
    deprecation <- Gen.option(alphaNumString)
    resources <- resources
    unstable <- arbitrary[Boolean]
  } yield Operator(
    name = name,
    executable = executable,
    category = category,
    help = help,
    description = description,
    inputs = inputs,
    outputs = outputs,
    deprecation = deprecation,
    unstable = unstable)

  val opPayload: Gen[OpPayload] = for {
    op <- alphaNumString
    seed <- arbitrary[Long]
    params <- Gen.listOf(attrValue)
    resources <- resources
  } yield OpPayload(op, seed, params, resources)

  val opResult: Gen[OpResult] = for {
    successful <- arbitrary[Boolean]
    artifacts <- Gen.listOf(attrValue)
    metrics <- Gen.listOf(metricValue)
    error <- Gen.option(errorDatum)
  } yield OpResult(successful, artifacts, metrics, error)

  private val source: Gen[Channel.Source] = {
    Gen.choose(0, 2).flatMap {
      case 0 => alphaNumString.map(v => Channel.Param(v))
      case 1 => value.map(v => Channel.Constant(v))
      case 2 => Gen.listOfN(2, alphaNumString).map(v => Channel.Reference(v.head, v.last))
    }
  }

  private val channel: Gen[Channel] = for {
    name <- alphaNumString
    source <- source
  } yield Channel(name, source)

  private val step: Gen[Step] = for {
    name <- alphaNumString
    op <- alphaNumString
    params <- Gen.listOf(channel)
  } yield Step(name, op, params)

  val workflow: Gen[Workflow] = for {
    name <- alphaNumString
    owner <- Gen.option(alphaNumString)
    contact <- Gen.option(alphaNumString)
    labels <- kvMap
    seed <- arbitrary[Long]
    params <- Gen.listOf(attrValue)
    steps <- Gen.listOf(step)
    repeat <- Gen.posNum[Int]
    resources <- resources
  } yield Workflow(
    name = name,
    owner = owner,
    contact = contact,
    labels = labels,
    seed = seed,
    params = params,
    steps = steps,
    repeat = repeat,
    resources = resources)
}
