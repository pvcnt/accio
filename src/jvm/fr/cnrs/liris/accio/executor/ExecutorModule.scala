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

package fr.cnrs.liris.accio.executor

import java.nio.file.Paths

import com.google.inject._
import com.twitter.finagle.Thrift
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.api.Operator
import fr.cnrs.liris.accio.core.domain.{OpMetaReader, OpRegistry}
import fr.cnrs.liris.accio.core.service._
import net.codingwell.scalaguice.ScalaMultibinder

object ExecutorModule extends TwitterModule {
  private[this] val addrFlag = flag[String]("addr", "Address of the Accio agent")

  override protected def configure(): Unit = {
    // Create an empty set of operators, in case nothing else is bound.
    ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Operator[_, _]]] {})

    // Bind remaining implementations.
    bind[OpMetaReader].to[ReflectOpMetaReader]
    bind[OpRegistry].to[RuntimeOpRegistry]
  }

  @Singleton
  @Provides
  def providesClient: AgentService.FinagledClient = {
    val service = Thrift.newService(addrFlag())
    new AgentService.FinagledClient(service)
  }

  @Provides
  def providesOpExecutor(opRegistry: RuntimeOpRegistry, opFactory: OpFactory, uploader: Uploader, downloader: Downloader): OpExecutor = {
    // Because the executor is designed to run inside a sandbox, we simply use current directory as temporary path
    // for the operator executor.
    //TODO: fixme, make this more configurable. TMPDIR is used by gridengine.
    val workDir = Paths.get(sys.env.get("TMPDIR").getOrElse("."))
    new OpExecutor(opRegistry, opFactory, uploader, downloader, workDir)
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[AgentService.FinagledClient].service.close()
  }
}