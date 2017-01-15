/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.infra.inject

import com.google.inject.{Provides, TypeLiteral}
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.service.{ReflectOpMetaReader, Scheduler, SchedulerService, StateManager}
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.common.getter.GetterModule
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

/**
 * Guice module provisioning main Accio services. It should be included in all Accio applications.
 */
object AccioCoreModule extends ScalaModule {
  override def configure(): Unit = {
    // Install mandatory modules.
    install(GetterModule)

    // Create a set binder for operators, in case no other module registers operators.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    // Bind trivial implementations.
    bind[OpMetaReader].to[ReflectOpMetaReader]
  }

  @Provides
  def providesSchedulerService(scheduler: Scheduler, stateManager: StateManager): SchedulerService = {
    new SchedulerService(scheduler: Scheduler, stateManager: StateManager)
  }

  @Provides
  def providesGraphFactory(opRegistry: OpRegistry): GraphFactory = {
    new GraphFactory(opRegistry)
  }

  @Provides
  def providesWorkflowFactory(graphFactory: GraphFactory, opRegistry: OpRegistry): WorkflowFactory = {
    new WorkflowFactory(graphFactory, opRegistry)
  }

  @Provides
  def providesRunFactory(workflowRepository: WorkflowRepository): RunFactory = {
    new RunFactory(workflowRepository)
  }
}