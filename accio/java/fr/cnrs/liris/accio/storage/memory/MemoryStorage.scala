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

package fr.cnrs.liris.accio.storage.memory

import java.util.concurrent.locks.ReentrantLock

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import fr.cnrs.liris.accio.storage._

/**
 * In-memory storage.
 *
 * @param statsReceiver Stats receiver.
 */
@Singleton
final class MemoryStorage @Inject()(statsReceiver: StatsReceiver) extends Storage {
  private[this] val workflowStore = new MemoryWorkflowRepository(statsReceiver)
  private[this] val runStore = new MemoryRunStore(statsReceiver)
  private[this] val storeProvider = new StoreProvider.Mutable {
    override def runs: RunStore.Mutable = runStore

    override def workflows: WorkflowStore.Mutable = workflowStore
  }
  private[this] val writeWaitStat = statsReceiver.stat("storage", "memory", "write_wait_nanos")
  private[this] val writeLock = new ReentrantLock

  override def read[T](fn: StoreProvider => T): T = fn(storeProvider)

  override def write[T](fn: StoreProvider.Mutable => T): T = {
    val start = System.nanoTime()
    writeLock.lock()
    try {
      writeWaitStat.add(System.nanoTime() - start)
      fn(storeProvider)
    } finally {
      writeLock.unlock()
    }
  }
}

object MemoryStorage {
  /**
   * Creates a new empty in-memory storage for use in testing.
   */
  def empty: Storage = new MemoryStorage(NullStatsReceiver)
}