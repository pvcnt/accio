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

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import com.twitter.util.Base64StringEncoder
import fr.cnrs.liris.accio.api.thrift.Task
import fr.cnrs.liris.accio.discovery.reflect.ReflectOpDiscovery
import fr.cnrs.liris.accio.service.OpExecutor
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

object AccioExecutorMain extends AccioExecutor

/**
 * Accio executor.
 */
class AccioExecutor {
  private[this] val protocolFactory = new TBinaryProtocol.Factory

  def main(args: Array[String]): Unit = {
    require(args.length == 2)
    val task = decode(args.head)
    com.twitter.jvm.numProcs.let(task.resource.cpu) {
      val opExecutor = new OpExecutor(new ReflectOpDiscovery)
      val taskExecutor = new TaskExecutor(opExecutor)
      taskExecutor.execute(task, Paths.get(args(1)))
    }
  }

  private def decode(str: String): Task = {
    val bytes = Base64StringEncoder.decode(str)
    val protocol = protocolFactory.getProtocol(new TIOStreamTransport(new ByteArrayInputStream(bytes)))
    Task.decode(protocol)
  }
}