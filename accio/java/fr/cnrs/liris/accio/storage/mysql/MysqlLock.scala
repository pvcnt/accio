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
import java.util.concurrent.locks.{Condition, Lock, ReentrantLock}

import com.twitter.finagle.mysql._
import com.twitter.util.Await
import com.twitter.util.logging.Logging

private[mysql] final class MysqlLock(client: Client, lockName: String) extends Lock with Logging {
  // Native MySQL locks are held per session. Finagle has a connection pooling feature, which means
  // that a session may potentially be shared and re-used. This is why we further add a reentrant
  // lock, to prevent multiple threads from acquiring the same lock.
  private[this] val reentrantLock = new ReentrantLock

  def lock(): Unit = {
    reentrantLock.lock()
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
      .ensure(reentrantLock.unlock())
      .unit
    Await.result(f)
  }

  override def newCondition(): Condition = ???

  override def tryLock(): Boolean = ???

  override def tryLock(time: Long, unit: TimeUnit): Boolean = ???

  override def lockInterruptibly(): Unit = ???

  private def decodeBoolean(row: Row): Boolean =
    row.values.head match {
      case ByteValue(b) => b != 0
      case ShortValue(s) => s != 0
      case IntValue(i) => i != 0
      case v => throw new RuntimeException(s"Unexpected boolean value: $v")
    }
}
