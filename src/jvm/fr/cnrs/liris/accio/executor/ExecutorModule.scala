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

package fr.cnrs.liris.accio.executor

import java.nio.file.Paths

import com.google.inject.{Injector, Provides, Singleton, TypeLiteral}
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain.{OpMetaReader, OpRegistry}
import fr.cnrs.liris.accio.core.service._
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

class ExecutorModule(addr: String) extends ScalaModule {
  override protected def configure(): Unit = {
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Provides
  def providesOpFactory(opRegistry: RuntimeOpRegistry, injector: Injector): OpFactory = {
    new OpFactory(opRegistry, injector: Injector)
  }

  @Singleton
  @Provides
  def providesClient: AgentService.FinagledClient = {
    val service = Thrift.newService(addr)
    new AgentService.FinagledClient(service)
  }

  @Provides
  def providesOpExecutor(opRegistry: RuntimeOpRegistry, opFactory: OpFactory, uploader: Uploader, downloader: Downloader): OpExecutor = {
    // Because the executor is designed to run inside a sandbox, we simply use current directory as temporary path
    // for the operator executor.
    new OpExecutor(opRegistry, opFactory, uploader, downloader, Paths.get("."))
  }
}