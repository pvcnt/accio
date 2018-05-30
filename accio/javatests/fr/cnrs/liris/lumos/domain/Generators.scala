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

import org.joda.time.Instant
import org.scalacheck.Arbitrary.{arbitrary, _}
import org.scalacheck.Gen

object Generators {
  val instant: Gen[Instant] = Gen.posNum[Long].map(v => new Instant(v))
  val alphaNumString: Gen[String] = arbitrary[String] //Gen.choose(1, 100).flatMap(n => Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString))
  val kvMap: Gen[Map[String, String]] = Gen.mapOf(Gen.alphaStr.flatMap(k => Gen.alphaStr.map(v => k -> v)))

  val execState: Gen[ExecStatus.State] = Gen.oneOf(ExecStatus.values)

  val execStatus: Gen[ExecStatus] = for {
    state <- execState
    time <- instant
    message <- Gen.option(Gen.alphaStr)
  } yield ExecStatus(state, time, message)

  val dataType: Gen[DataType] = Gen.oneOf(DataType.values)

  val remoteFile: Gen[RemoteFile] = for {
    uri <- Gen.alphaStr
    contentType <- Gen.option(alphaNumString)
    format <- Gen.option(alphaNumString)
    sha256 <- Gen.option(alphaNumString)
  } yield RemoteFile(uri, contentType, format, sha256)

  val value: Gen[Value] = dataType.flatMap(genValue)

  val attrValue: Gen[AttrValue] = for {
    name <- alphaNumString
    value <- value
    aspects <- Gen.listOf(alphaNumString)
  } yield AttrValue(name, value, aspects.toSet)

  val errorDatum: Gen[ErrorDatum] = for {
    mnemonic <- alphaNumString
    message <- Gen.option(Gen.alphaStr)
    stacktrace <- Gen.listOf(Gen.alphaStr)
  } yield ErrorDatum(mnemonic, message, stacktrace)

  val metricValue: Gen[MetricValue] = for {
    name <- alphaNumString
    value <- arbitrary[Double]
    aspects <- Gen.listOf(alphaNumString)
  } yield MetricValue(name, value, aspects.toSet)

  val task: Gen[Task] = for {
    name <- alphaNumString
    mnemonic <- Gen.option(alphaNumString)
    dependencies <- Gen.listOf(alphaNumString)
    metadata <- kvMap
    exitCode <- Gen.option(arbitrary[Int])
    metrics <- Gen.listOf(metricValue)
    error <- Gen.option(errorDatum)
    status <- execStatus
    history <- Gen.listOf(execStatus)
  } yield Task(
    name = name,
    mnemonic = mnemonic,
    dependencies = dependencies.toSet,
    metadata = metadata,
    exitCode = exitCode,
    metrics = metrics,
    error = error,
    status = status,
    history = history)

  val job: Gen[Job] = for {
    name <- alphaNumString
    createTime <- instant
    owner <- Gen.option(alphaNumString)
    contact <- Gen.option(alphaNumString)
    labels <- kvMap
    metadata <- kvMap
    inputs <- Gen.listOf(attrValue)
    outputs <- Gen.listOf(attrValue)
    progress <- Gen.chooseNum(0, 100)
    tasks <- Gen.listOf(task)
    status <- execStatus
    history <- Gen.listOf(execStatus)
  } yield Job(
    name = name,
    createTime = createTime,
    owner = owner,
    contact = contact,
    labels = labels,
    metadata = metadata,
    inputs = inputs,
    outputs = outputs,
    progress = progress,
    tasks = tasks,
    status = status,
    history = history)

  val eventPayload: Gen[Event.Payload] = {
    Gen.choose(0, 8).flatMap {
      case 0 => job.map(v => Event.JobEnqueued(v))
      case 1 => Gen.listOf(task).map(v => Event.JobExpanded(v))
      case 2 =>
        for {
          metadata <- kvMap
          message <- Gen.option(Gen.alphaStr)
        } yield Event.JobScheduled(metadata, message)
      case 3 => Gen.option(Gen.alphaStr).map(v => Event.JobStarted(v))
      case 4 =>
        for {
          outputs <- Gen.listOf(attrValue)
          message <- Gen.option(Gen.alphaStr)
        } yield Event.JobCompleted(outputs, message)
      case 5 => Gen.option(Gen.alphaStr).map(v => Event.JobCanceled(v))
      case 6 =>
        for {
          name <- alphaNumString
          metadata <- kvMap
          message <- Gen.option(Gen.alphaStr)
        } yield Event.TaskScheduled(name, metadata, message)
      case 7 =>
        for {
          name <- alphaNumString
          message <- Gen.option(Gen.alphaStr)
        } yield Event.TaskStarted(name, message)
      case 8 =>
        for {
          name <- alphaNumString
          exitCode <- arbitrary[Int]
          metrics <- Gen.listOf(metricValue)
          error <- Gen.option(errorDatum)
          message <- Gen.option(Gen.alphaStr)
        } yield Event.TaskCompleted(name, exitCode, metrics, error, message)
    }
  }

  val event: Gen[Event] = for {
    parent <- alphaNumString
    sequence <- Gen.posNum[Long]
    time <- instant
    payload <- eventPayload
  } yield Event(parent, sequence, time, payload)

  private def genValue(dataType: DataType): Gen[Value] =
    dataType match {
      case DataType.Int => arbitrary[Int].map(v => Value.Int(v))
      case DataType.Long => arbitrary[Long].map(v => Value.Long(v))
      case DataType.Float => arbitrary[Float].map(v => Value.Float(v))
      case DataType.Double => arbitrary[Double].map(v => Value.Double(v))
      case DataType.String => Gen.alphaStr.map(v => Value.String(v))
      case DataType.Bool => arbitrary[Boolean].map(v => Value.Bool(v))
      case DataType.File => remoteFile.map(v => Value.File(v))
      case DataType.Dataset => remoteFile.map(v => Value.Dataset(v))
      case _: DataType.UserDefined => throw new AssertionError // No custom type is registered, should not happen.
    }
}
