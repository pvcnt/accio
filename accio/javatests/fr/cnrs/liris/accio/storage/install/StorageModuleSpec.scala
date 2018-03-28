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

package fr.cnrs.liris.accio.storage.install

import com.google.inject.Module
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.inject.{CreateTwitterInjector, TwitterModule}
import fr.cnrs.liris.accio.storage.Storage
import fr.cnrs.liris.accio.storage.memory.MemoryStorage
import fr.cnrs.liris.accio.storage.mysql.MysqlStorage
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[StorageModule]].
 */
class StorageModuleSpec extends UnitSpec with CreateTwitterInjector {
  behavior of "StorageModule"

  override protected def modules: Seq[Module] = Seq(StorageModule, StatsModule)

  it should "provide a default memory storage" in {
    val injector = createInjector()
    injector.instance[Storage] shouldBe a[MemoryStorage]
  }

  it should "provide a memory storage" in {
    val injector = createInjector("-storage.type", "memory")
    injector.instance[Storage] shouldBe a[MemoryStorage]
  }

  it should "provide a MySQL storage" in {
    val injector = createInjector("-storage.type", "mysql")
    injector.instance[Storage] shouldBe a[MysqlStorage]
  }

  private object StatsModule extends TwitterModule {
    override protected def configure(): Unit = {
      bind[StatsReceiver].to[NullStatsReceiver]
    }
  }

}