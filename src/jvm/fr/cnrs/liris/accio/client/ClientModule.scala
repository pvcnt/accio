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

package fr.cnrs.liris.accio.client

import com.google.inject.{AbstractModule, Provides, Singleton, TypeLiteral}
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.client.command._
import fr.cnrs.liris.accio.client.service.RemoteOpRegistry
import fr.cnrs.liris.accio.core.domain.OpRegistry
import fr.cnrs.liris.accio.core.infra.cli.Command
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

/**
 * Guice module providing specific bindings for the Accio CLI application.
 */
object ClientModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val commands = ScalaMultibinder.newSetBinder(binder, new TypeLiteral[Class[_ <: Command]] {})
    commands.addBinding.toInstance(classOf[ExportCommand])
    commands.addBinding.toInstance(classOf[HelpCommand])
    commands.addBinding.toInstance(classOf[ListCommand])
    commands.addBinding.toInstance(classOf[LogsCommand])
    commands.addBinding.toInstance(classOf[PsCommand])
    commands.addBinding.toInstance(classOf[PushCommand])
    commands.addBinding.toInstance(classOf[SubmitCommand])
    commands.addBinding.toInstance(classOf[ValidateCommand])
    commands.addBinding.toInstance(classOf[VersionCommand])

    bind[OpRegistry].to[RemoteOpRegistry]
  }

  @Singleton
  @Provides
  def providesClient: AgentService.FinagledClient = {
    //TODO: inject addr.
    val service = Thrift.newService("localhost:9999")
    sys.addShutdownHook(service.close())
    new AgentService.FinagledClient(service)
  }
}