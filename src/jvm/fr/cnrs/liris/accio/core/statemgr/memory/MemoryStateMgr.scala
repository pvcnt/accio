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

package fr.cnrs.liris.accio.core.statemgr.memory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

import com.google.inject.Singleton
import fr.cnrs.liris.accio.core.statemgr.{Lock, StateManager}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * State manager designed for a single-node deployment.
 */
@Singleton
final class MemoryStateMgr extends StateManager {
  private[this] val locks = mutable.WeakHashMap.empty[String, JavaLock]
  //private[this] val store = new ConcurrentHashMap[String, Array[Byte]].asScala

  override def lock(key: String): Lock = synchronized {
    locks.getOrElseUpdate(key, new JavaLock(key))
  }

  /*override def get(key: String): Option[Array[Byte]] = store.get(key)

  override def set(key: String, value: Array[Byte]): Unit = {
    store(key) = value
  }

  override def list(key: String): Set[String] = store.keySet.filter(_.startsWith(key + '/')).toSet*/

  /**
   * Lock implementation using local files to synchronize. It is *NOT* reentrant.
   *
   * @param key Lock key.
   */
  private class JavaLock(key: String) extends Lock {
    private[this] val javaLock = new ReentrantLock

    override def lock(): Unit = javaLock.lock()

    override def tryLock(): Boolean = javaLock.tryLock()

    override def unlock(): Unit = javaLock.unlock()
  }

}