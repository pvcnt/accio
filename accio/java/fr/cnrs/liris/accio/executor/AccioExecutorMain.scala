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

package fr.cnrs.liris.accio.executor

import java.nio.file.Paths

import com.twitter.inject.app.App
import com.twitter.util.Await
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.discovery.{DiscoveryModule, OpRegistry}
import fr.cnrs.liris.accio.domain.DataTypes
import fr.cnrs.liris.accio.domain.thrift.{ThriftAdapter, Workflow}
import fr.cnrs.liris.lumos.transport.{EventTransport, EventTransportModule}
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

import scala.util.control.NonFatal

object AccioExecutorMain extends AccioExecutor

/**
 * Accio executor.
 */
class AccioExecutor extends App with Logging {
  override protected def modules = Seq(EventTransportModule, DiscoveryModule)

  override protected def failfastOnFlagsNotParsed = true

  override protected def run(): Unit = {
    require(args.length == 1, "There should be a single argument")
    val workflow = try {
      ThriftAdapter.toDomain(BinaryScroogeSerializer.fromString(args.head, Workflow))
    } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to read workflow from ${args.head}", e)
        sys.exit(1)
    }
    val eventTransport = injector.instance[EventTransport]
    val opRegistry = injector.instance[OpRegistry]
    val executor = new WorkflowExecutor(workflow, Paths.get("."), opRegistry, eventTransport)
    Await.ready(executor.execute())
    executor.close()
  }

  init {
    DataTypes.register()
  }
}