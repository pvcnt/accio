/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.lumos.transport

import java.nio.file.Paths

import com.google.inject.{Provider, Singleton}
import com.twitter.conversions.time._
import com.twitter.finagle.Thrift
import com.twitter.finagle.thrift.RichClientParam
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.util.Await
import fr.cnrs.liris.lumos.server.LumosService
import net.codingwell.scalaguice.ScalaMultibinder

object EventTransportModule extends TwitterModule {
  // Binary file transport.
  private[this] val binaryFile = flag[String]("event.binary.file", "File where to write Lumos events in Thrift binary format")

  // Text file transport.
  private[this] val textFile = flag[String]("event.text.file", "File where to write Lumos events in Thrift text format")

  // Lumos service transport.
  private[this] val serverAddress = flag[String]("event.server.address", "Address to a server where to send Lumos events")
  private[this] val serverTimeout = flag("event.server.timeout", 10.seconds, "Timeout when contacting the server")

  def args: Seq[String] = {
    flags.filter(_.isDefined).flatMap(flag => Seq(s"-${flag.name}", flag.apply().toString))
  }

  override def configure(): Unit = {
    val transports = ScalaMultibinder.newSetBinder[EventTransport](binder)
    if (binaryFile.isDefined) {
      transports.addBinding.toProvider[BinaryFileEventTransportProvider].in[Singleton]
    }
    if (textFile.isDefined) {
      transports.addBinding.toProvider[TextFileEventTransportProvider].in[Singleton]
    }
    if (serverAddress.isDefined) {
      transports.addBinding.toProvider[LumosServiceEventTransportProvider].in[Singleton]
    }
    bind[EventTransport].to[EventTransportMultiplexer]
  }

  private class BinaryFileEventTransportProvider extends Provider[BinaryFileEventTransport] {
    override def get(): BinaryFileEventTransport = {
      new BinaryFileEventTransport(Paths.get(binaryFile()))
    }
  }

  private class TextFileEventTransportProvider extends Provider[TextFileEventTransport] {
    override def get(): TextFileEventTransport = {
      new TextFileEventTransport(Paths.get(textFile()))
    }
  }

  private class LumosServiceEventTransportProvider extends Provider[LumosServiceEventTransport] {
    override def get(): LumosServiceEventTransport = {
      val service = Thrift.client
        .withRequestTimeout(serverTimeout())
        .withSessionQualifier.noFailFast
        .withSessionQualifier.noFailureAccrual
        .newService(serverAddress())
      val params = RichClientParam()
      val client = LumosService.MethodPerEndpoint(LumosService.ServicePerEndpointBuilder.servicePerEndpoint(service, params))
      new LumosServiceEventTransport(client)
    }
  }

  override def singletonShutdown(injector: Injector): Unit = {
    Await.ready(injector.instance[EventTransportMultiplexer].close())
  }
}
