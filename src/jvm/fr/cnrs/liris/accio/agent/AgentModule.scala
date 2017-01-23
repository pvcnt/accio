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

package fr.cnrs.liris.accio.agent

import com.google.inject.{Provides, Singleton, TypeLiteral}
import com.twitter.inject.TwitterModule
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.accio.core.service._
import net.codingwell.scalaguice.ScalaMultibinder

/**
 * Guice module provisioning services for the Accio agent.
 */
object AgentModule extends TwitterModule {
  private[this] val clusterFlag = flag("cluster", "default", "Cluster name") // Not used yet.
  private[this] val taskTimeout = flag("task_timeout", Duration.fromSeconds(30), "Time after which a task is considered lost")

  protected override def configure(): Unit = {
    // Create an empty set of operators, in case nothing else is bound.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    // Bind remaining implementations.
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Singleton
  @Provides
  def providesLostTaskObserver(stateManager: StateManager, runRepository: RunRepository, runManager: RunLifecycleManager): LostTaskObserver = {
    new LostTaskObserver(taskTimeout(), stateManager, runRepository, runManager)
  }
}