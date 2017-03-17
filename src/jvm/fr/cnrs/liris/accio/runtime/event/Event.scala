/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.runtime.event

case class Event(kind: EventKind, bytes: Array[Byte], tag: Option[String]) {
  def message: String = new String(bytes)
}

object Event {
  def apply(kind: EventKind, bytes: Array[Byte]): Event = new Event(kind, bytes, None)

  def apply(kind: EventKind, message: String): Event = new Event(kind, message.getBytes, None)

  def apply(kind: EventKind, message: String, tag: Option[String]): Event = new Event(kind, message.getBytes, tag)

  def error(message: String): Event = Event(EventKind.Error, message, None)

  def error(message: String, tag: String): Event = Event(EventKind.Error, message, Some(tag))

  def warn(message: String): Event = Event(EventKind.Warning, message, None)

  def warn(message: String, tag: String): Event = Event(EventKind.Warning, message, Some(tag))

  def info(message: String): Event = Event(EventKind.Info, message, None)

  def info(message: String, tag: String): Event = Event(EventKind.Info, message, Some(tag))

  def progress(message: String): Event = Event(EventKind.Progress, message, None)

  def progress(message: String, tag: String): Event = Event(EventKind.Progress, message, Some(tag))

  def start(message: String): Event = Event(EventKind.Start, message, None)

  def start(message: String, tag: String): Event = Event(EventKind.Start, message, Some(tag))

  def finish(message: String): Event = Event(EventKind.Finish, message, None)

  def finish(message: String, tag: String): Event = Event(EventKind.Finish, message, Some(tag))
}

sealed trait EventKind

object EventKind {

  case object Error extends EventKind

  case object Warning extends EventKind

  case object Info extends EventKind

  case object Progress extends EventKind

  case object Start extends EventKind

  case object Finish extends EventKind

  case object Stdout extends EventKind

  case object Stderr extends EventKind

  val ErrorsWarningsInfosOutput = Set(
    EventKind.Error,
    EventKind.Warning,
    EventKind.Info,
    EventKind.Stdout,
    EventKind.Stderr)

}