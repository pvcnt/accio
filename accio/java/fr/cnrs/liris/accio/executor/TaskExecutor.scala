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

import com.google.inject.{Inject, Singleton}
import com.twitter.util.logging.Logging
import fr.cnrs.liris.accio.api.thrift.{OpPayload, OpResult}
import fr.cnrs.liris.accio.runtime.OpExecutor
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

@Singleton
final class TaskExecutor @Inject()(opExecutor: OpExecutor) extends Logging {
  def execute(payload: OpPayload, outputFile: Path): Boolean = {
    val result = execute(payload)
    Files.createDirectories(outputFile.getParent)
    val fos = new FileOutputStream(outputFile.toFile)
    try {
      BinaryScroogeSerializer.write(result, fos)
    } finally {
      fos.close()
    }
    result.successful
  }

  private def execute(payload: OpPayload): OpResult = {
    logger.info(s"Starting execution of operator ${payload.op}")
    try {
      opExecutor.execute(payload)
    } catch {
      case e: Throwable =>
        // Normally, operator executor is supposed to be robust enough to catch all errors. But we
        // still handle and uncaught error here, just in case...
        logger.error(s"Operator raised an unexpected error", e)
        OpResult(successful = false)
    }
  }
}