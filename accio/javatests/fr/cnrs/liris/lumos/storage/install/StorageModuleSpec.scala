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

package fr.cnrs.liris.lumos.storage.install

import com.google.inject.Module
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.inject.{CreateTwitterInjector, TwitterModule}
import fr.cnrs.liris.lumos.storage.JobStore
import fr.cnrs.liris.lumos.storage.memory.MemoryJobStore
import fr.cnrs.liris.lumos.storage.mysql.MysqlJobStore
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[StorageModule]].
 */
class StorageModuleSpec extends UnitSpec with CreateTwitterInjector {
  behavior of "StorageModule"

  override protected def modules: Seq[Module] = Seq(StorageModule, StatsModule)

  it should "provide a default memory storage" in {
    val injector = createInjector()
    injector.instance[JobStore] shouldBe a[MemoryJobStore]
  }

  it should "provide a memory storage" in {
    val injector = createInjector("-storage", "memory")
    injector.instance[JobStore] shouldBe a[MemoryJobStore]
  }

  it should "provide a MySQL storage" in {
    val injector = createInjector("-storage", "mysql")
    injector.instance[JobStore] shouldBe a[MysqlJobStore]
  }

  private object StatsModule extends TwitterModule {
    override protected def configure(): Unit = {
      bind[StatsReceiver].to[NullStatsReceiver]
    }
  }

}