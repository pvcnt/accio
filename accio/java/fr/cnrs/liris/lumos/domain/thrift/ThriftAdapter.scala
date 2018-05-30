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

package fr.cnrs.liris.lumos.domain.thrift

import com.github.nscala_time.time.Imports._
import fr.cnrs.liris.lumos.domain
import fr.cnrs.liris.lumos.domain.DataType
import org.joda.time.Instant

object ThriftAdapter {
  def toDomain(obj: Value): domain.Value = {
    val value = obj.payload match {
      case ValuePayload.Int(v) => domain.Value.Int(v)
      case ValuePayload.Long(v) => domain.Value.Long(v)
      case ValuePayload.Float(v) => domain.Value.Float(v.toFloat)
      case ValuePayload.Dbl(v) => domain.Value.Double(v)
      case ValuePayload.Boolean(v) => domain.Value.Bool(v)
      case ValuePayload.Str(v) => domain.Value.String(v)
      case ValuePayload.File(v) => domain.Value.File(toDomain(v))
      case ValuePayload.UnknownUnionField(_) => throw new IllegalArgumentException("Illegal value")
    }
    DataType.parse(obj.dataType) match {
      case None => domain.Value.Unresolved(value, obj.dataType)
      case Some(dataType) => value.cast(dataType).getOrElse {
        throw new IllegalArgumentException(s"Invalid value for data type $dataType: $value")
      }
    }
  }

  def toDomain(obj: Job): domain.Job = {
    domain.Job(
      name = obj.name,
      createTime = new Instant(obj.createTime),
      labels = obj.labels.toMap,
      metadata = obj.metadata.toMap,
      owner = obj.owner,
      contact = obj.contact,
      inputs = obj.inputs.map(toDomain),
      outputs = obj.outputs.map(toDomain),
      tasks = obj.tasks.map(toDomain),
      progress = obj.progress,
      status = obj.status.map(toDomain).getOrElse(domain.ExecStatus()),
      history = obj.history.map(toDomain))
  }

  def toDomain(obj: Event): domain.Event = {
    val payload = obj.payload match {
      case EventPayload.JobEnqueued(e) => domain.Event.JobEnqueued(toDomain(e.job))
      case EventPayload.JobExpanded(e) => domain.Event.JobExpanded(e.tasks.map(toDomain))
      case EventPayload.JobScheduled(e) => domain.Event.JobScheduled(e.metadata.toMap, e.message)
      case EventPayload.JobStarted(e) => domain.Event.JobStarted(e.message)
      case EventPayload.JobCanceled(e) => domain.Event.JobCanceled(e.message)
      case EventPayload.JobCompleted(e) => domain.Event.JobCompleted(e.outputs.map(toDomain), e.message)
      case EventPayload.TaskScheduled(e) => domain.Event.TaskScheduled(e.name, e.metadata.toMap, e.message)
      case EventPayload.TaskStarted(e) => domain.Event.TaskStarted(e.name, e.message)
      case EventPayload.TaskCompleted(e) =>
        domain.Event.TaskCompleted(e.name, e.exitCode, e.metrics.map(toDomain), e.error.map(toDomain), e.message)
      case EventPayload.UnknownUnionField(_) => throw new IllegalArgumentException("Illegal value")
    }
    domain.Event(obj.parent, obj.sequence, new Instant(obj.time), payload)
  }

  def toDomain(obj: ExecState): domain.ExecStatus.State =
    obj match {
      case ExecState.Pending => domain.ExecStatus.Pending
      case ExecState.Scheduled => domain.ExecStatus.Scheduled
      case ExecState.Running => domain.ExecStatus.Running
      case ExecState.Successful => domain.ExecStatus.Successful
      case ExecState.Failed => domain.ExecStatus.Failed
      case ExecState.Canceled => domain.ExecStatus.Canceled
      case ExecState.Lost => domain.ExecStatus.Lost
      case ExecState.EnumUnknownExecState(_) => throw new IllegalArgumentException("Illegal value")
    }

  def toDomain(obj: AttrValue): domain.AttrValue = {
    domain.AttrValue(obj.name, toDomain(obj.value), obj.aspects.toSet)
  }

  def toDomain(obj: RemoteFile): domain.RemoteFile = {
    domain.RemoteFile(
      uri = obj.uri,
      contentType = obj.contentType,
      format = obj.format,
      sha256 = obj.sha256)
  }

  private def toDomain(obj: Task): domain.Task = {
    domain.Task(
      name = obj.name,
      mnemonic = obj.mnemonic,
      dependencies = obj.dependencies.toSet,
      metadata = obj.metadata.toMap,
      status = obj.status.map(toDomain).getOrElse(domain.ExecStatus()),
      history = obj.history.map(toDomain),
      exitCode = obj.exitCode,
      error = obj.error.map(toDomain),
      metrics = obj.metrics.map(toDomain))
  }

  private def toDomain(obj: ExecStatus): domain.ExecStatus = {
    domain.ExecStatus(toDomain(obj.state), new Instant(obj.time), obj.message)
  }

  def toDomain(obj: ErrorDatum): domain.ErrorDatum = {
    domain.ErrorDatum(obj.mnemonic, obj.message, obj.stacktrace)
  }

  def toDomain(obj: MetricValue): domain.MetricValue = {
    domain.MetricValue(obj.name, obj.value, obj.aspects.toSet)
  }

  def toThrift(obj: domain.Value): Value = {
    obj match {
      case domain.Value.Int(v) => Value(obj.dataType.name, ValuePayload.Int(v))
      case domain.Value.Long(v) => Value(obj.dataType.name, ValuePayload.Long(v))
      case domain.Value.Float(v) => Value(obj.dataType.name, ValuePayload.Float(v))
      case domain.Value.Double(v) => Value(obj.dataType.name, ValuePayload.Dbl(v))
      case domain.Value.String(v) => Value(obj.dataType.name, ValuePayload.Str(v))
      case domain.Value.Bool(v) => Value(obj.dataType.name, ValuePayload.Boolean(v))
      case domain.Value.File(v) => Value(obj.dataType.name, ValuePayload.File(toThrift(v)))
      case domain.Value.Dataset(v) => Value(obj.dataType.name, ValuePayload.File(toThrift(v)))
      case domain.Value.Unresolved(value, originalDataType) => Value(originalDataType, toThrift(value).payload)
      case domain.Value.UserDefined(v, dataType) =>
        val encoded = dataType.encode(v.asInstanceOf[dataType.JvmType])
        //val dataTypeName = s"${dataType.name}:${encoded.dataType.name}"
        Value(dataType.name, toThrift(encoded).payload)
    }
  }

  def toThrift(obj: domain.Job): Job = {
    Job(
      name = obj.name,
      createTime = obj.createTime.millis,
      labels = obj.labels,
      metadata = obj.metadata,
      owner = obj.owner,
      contact = obj.contact,
      inputs = obj.inputs.map(toThrift),
      outputs = obj.outputs.map(toThrift),
      progress = obj.progress,
      tasks = obj.tasks.map(toThrift),
      status = Some(toThrift(obj.status)),
      history = obj.history.map(toThrift))
  }

  def toThrift(obj: domain.Event): Event = {
    val payload = obj.payload match {
      case e: domain.Event.JobEnqueued =>
        EventPayload.JobEnqueued(JobEnqueuedEvent(toThrift(e.job)))
      case e: domain.Event.JobExpanded =>
        EventPayload.JobExpanded(JobExpandedEvent(e.tasks.map(toThrift)))
      case e: domain.Event.JobScheduled =>
        EventPayload.JobScheduled(JobScheduledEvent(e.metadata, e.message))
      case e: domain.Event.JobStarted => EventPayload.JobStarted(JobStartedEvent(e.message))
      case e: domain.Event.JobCanceled => EventPayload.JobCanceled(JobCanceledEvent(e.message))
      case e: domain.Event.JobCompleted =>
        EventPayload.JobCompleted(JobCompletedEvent(e.outputs.map(toThrift), e.message))
      case e: domain.Event.TaskScheduled =>
        EventPayload.TaskScheduled(TaskScheduledEvent(e.name, e.metadata, e.message))
      case e: domain.Event.TaskStarted => EventPayload.TaskStarted(TaskStartedEvent(e.name, e.message))
      case e: domain.Event.TaskCompleted =>
        EventPayload.TaskCompleted(TaskCompletedEvent(e.name, e.exitCode, e.metrics.map(toThrift), e.error.map(toThrift), e.message))
    }
    Event(obj.parent, obj.sequence, obj.time.millis, payload)
  }

  def toThrift(obj: domain.AttrValue): AttrValue = {
    AttrValue(obj.name, toThrift(obj.value), obj.aspects)
  }

  private def toThrift(obj: domain.Task): Task = {
    Task(
      name = obj.name,
      mnemonic = obj.mnemonic,
      dependencies = obj.dependencies,
      metadata = obj.metadata,
      exitCode = obj.exitCode,
      metrics = obj.metrics.map(toThrift),
      error = obj.error.map(toThrift),
      status = Some(toThrift(obj.status)),
      history = obj.history.map(toThrift))
  }

  private def toThrift(obj: domain.ExecStatus): ExecStatus = {
    ExecStatus(toThrift(obj.state), obj.time.millis, obj.message)
  }

  def toThrift(obj: domain.ExecStatus.State): ExecState =
    obj match {
      case domain.ExecStatus.Pending => ExecState.Pending
      case domain.ExecStatus.Scheduled => ExecState.Scheduled
      case domain.ExecStatus.Running => ExecState.Running
      case domain.ExecStatus.Successful => ExecState.Successful
      case domain.ExecStatus.Failed => ExecState.Failed
      case domain.ExecStatus.Canceled => ExecState.Canceled
      case domain.ExecStatus.Lost => ExecState.Lost
    }

  def toThrift(obj: domain.ErrorDatum): ErrorDatum = {
    ErrorDatum(obj.mnemonic, obj.message, obj.stacktrace)
  }

  def toThrift(obj: domain.MetricValue): MetricValue = {
    MetricValue(obj.name, obj.value, obj.aspects)
  }

  def toThrift(obj: domain.RemoteFile): RemoteFile = {
    RemoteFile(
      uri = obj.uri,
      contentType = obj.contentType,
      format = obj.format,
      sha256 = obj.sha256)
  }
}