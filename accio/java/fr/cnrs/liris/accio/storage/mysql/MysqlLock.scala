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

package fr.cnrs.liris.accio.storage.mysql

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.{Condition, Lock}

import com.twitter.finagle.mysql._
import com.twitter.util.Await
import com.twitter.util.logging.Logging

private[mysql] final class MysqlLock(client: Client, lockName: String) extends Lock with Logging {
  def lock(): Unit = {
    val f = client
      .prepare("select get_lock(?, -1)")
      .select(lockName)(decodeBoolean)
      .map(_.head)
      .unit
    Await.result(f)
  }

  def unlock(): Unit = {
    val f = client
      .prepare("select release_lock(?)")
      .apply(lockName)
      .map {
        case ResultSet(_, _) => // Do nothing
        case err => logger.error(s"Failed to release lock: $err")
      }
      .unit
    Await.result(f)
  }

  override def newCondition(): Condition = ???

  override def tryLock(): Boolean = {
    val f = client
      .prepare("select get_lock(?, 0)")
      .select(lockName)(decodeBoolean)
      .map(_.head)
    Await.result(f)
  }

  override def tryLock(time: Long, unit: TimeUnit): Boolean = {
    val f = client
      .prepare("select get_lock(?, ?)")
      .select(lockName, unit.toMillis(time).toDouble / 1000)(decodeBoolean)
      .map(_.head)
    Await.result(f)
  }

  override def lockInterruptibly(): Unit = ???

  private def decodeBoolean(row: Row): Boolean =
    row.values.head match {
      case ByteValue(b) => b != 0
      case ShortValue(s) => s != 0
      case IntValue(i) => i != 0
      case v => throw new RuntimeException(s"Unexpected boolean value: $v")
    }
}
