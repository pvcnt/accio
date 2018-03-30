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

package fr.cnrs.liris.accio.agent

import java.util.concurrent.Executors

import com.google.common.eventbus.{AsyncEventBus, EventBus}
import com.google.inject.{Provides, Singleton}
import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.auth.AuthModule
import fr.cnrs.liris.accio.config.ConfigModule
import fr.cnrs.liris.accio.discovery.inject.DiscoveryModule
import fr.cnrs.liris.accio.dsl.DslModule
import fr.cnrs.liris.accio.scheduler.inject.SchedulerModule
import fr.cnrs.liris.accio.service.{OpRegistry, RuntimeOpRegistry}
import fr.cnrs.liris.accio.storage.install.StorageModule

object AgentServerModule extends TwitterModule {
  override def modules = Seq(
    AuthModule,
    DiscoveryModule,
    ConfigModule,
    DslModule,
    SchedulerModule,
    StorageModule)

  override def configure(): Unit = {
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Provides
  @Singleton
  def providesEventBus(): EventBus = {
    val executor = Executors.newCachedThreadPool(new NamedPoolThreadFactory("event-bus"))
    new AsyncEventBus(executor)
  }

  override def singletonStartup(injector: Injector): Unit = {
    val eventBus = injector.instance[EventBus]
    eventBus.register(injector.instance[SchedulerListener])
    eventBus.register(injector.instance[RunListener])
  }
}
