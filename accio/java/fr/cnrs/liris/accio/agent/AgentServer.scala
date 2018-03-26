/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the ter ms of the GNU General Public License as published by
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

import java.net.InetSocketAddress

import com.twitter.conversions.time._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.RichServerParam
import com.twitter.finagle.{ListeningServer, Service, Thrift}
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.modules.{LoggerModule, StatsReceiverModule}
import com.twitter.inject.server.TwitterServer
import com.twitter.util.Await
import fr.cnrs.liris.accio.logging.LogbackConfigurator

object AgentServerMain extends AgentServer

class AgentServer extends TwitterServer with LogbackConfigurator {
  private[this] val addrFlag = flag("thrift.port", new InetSocketAddress(9999), "Thrift server port")
  private[this] val announceFlag = flag[String]("thrift.announce", "Address for announcing the Thrift server")

  override def failfastOnFlagsNotParsed = true

  override def modules = Seq(AgentServerModule, StatsReceiverModule, LoggerModule)

  override def thriftPort: Option[Int] = addrFlag.getWithDefault.map(_.getPort)

  @Lifecycle
  override final def postWarmup() {
    super.postWarmup()
    val httpServer = startServer()
    await(httpServer)
    info(s"Thrift server started on port ${thriftPort.get}")

    // Announce the HTTP server if required.
    announceFlag.get.foreach(httpServer.announce)
  }

  private def startServer(): ListeningServer = {
    val params = RichServerParam(serverStats = injector.instance[StatsReceiver])
    var service: Service[Array[Byte], Array[Byte]] = new AgentService$FinagleService(injector.instance[AgentServiceImpl], params)
    // TODO: add filters (at least stats, access logging and exception mapping).
    // service = injector.instance[StatsFilter[Array[Byte], Array[Byte]]].andThen(service)

    val server = Thrift.server
      .withStatsReceiver(injector.instance[StatsReceiver])
      .withLabel("thrift")
      .serve(addrFlag(), service)

    onExit {
      Await.result(server.close(60.seconds.fromNow))
    }
    server
  }
}