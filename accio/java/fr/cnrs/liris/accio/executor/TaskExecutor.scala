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

import java.io.FileOutputStream
import java.nio.file.{Files, Path}

import com.twitter.inject.Logging
import fr.cnrs.liris.accio.api.Errors
import fr.cnrs.liris.accio.api.thrift.{OpResult, Task}
import fr.cnrs.liris.accio.service.{OpExecutor, OpExecutorOpts}
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

final class TaskExecutor(opExecutor: OpExecutor) extends Logging {
  private[this] val protocolFactory = new TBinaryProtocol.Factory

  def execute(task: Task, output: Path): Unit = {
    val result = execute(task)
    write(result, output)
  }

  private def execute(task: Task): OpResult = {
    logger.info(s"Starting execution of task ${task.id.value}")
    try {
      val opts = OpExecutorOpts(useProfiler = true)
      opExecutor.execute(task.payload, opts)
    } catch {
      case e: Throwable =>
        // Normally, operator executor is supposed to be robust enough to catch all errors. But we still handle
        // and uncaught error here, just in case...
        logger.error(s"Operator raised an unexpected error", e)
        OpResult(-999, Some(Errors.create(e)))
    }
  }

  private def write(result: OpResult, to: Path): Unit = {
    Files.createDirectories(to.getParent)
    val fos = new FileOutputStream(to.toFile)
    try {
      val protocol = protocolFactory.getProtocol(new TIOStreamTransport(fos))
      result.write(protocol)
    } finally {
      fos.close()
    }
  }
}