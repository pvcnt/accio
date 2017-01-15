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

import java.nio.file.Path

import com.google.inject.Provides
import com.twitter.finagle.Thrift
import fr.cnrs.liris.accio.agent.AgentService
import fr.cnrs.liris.accio.core.service.{OpExecutor, Uploader}
import fr.cnrs.liris.accio.core.domain.{OpFactory, RuntimeOpRegistry}
import fr.cnrs.liris.common.flags.inject.InjectFlag
import fr.cnrs.liris.common.getter.DownloadClient
import net.codingwell.scalaguice.ScalaModule

object ExecutorModule extends ScalaModule {
  override protected def configure(): Unit = {}

  @Provides
  def providesAgentClient(
    @InjectFlag("agent_addr") agentAddr: String): AgentService.FinagledClient = {
    val service = Thrift.newService(agentAddr)
    new AgentService.FinagledClient(service)
  }

  @Provides
  def providesOpExecutor(opRegistry: RuntimeOpRegistry, opFactory: OpFactory, uploader: Uploader, downloader: DownloadClient, @InjectFlag("workdir") workDir: Path): OpExecutor = {
    new OpExecutor(opRegistry, opFactory, uploader, downloader, workDir)
  }
}