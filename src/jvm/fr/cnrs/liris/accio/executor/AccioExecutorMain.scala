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

import com.google.inject.{Guice, Module}
import com.twitter.util.Await
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.domain.TaskId
import fr.cnrs.liris.accio.core.infra.downloader.GetterDownloaderModule
import fr.cnrs.liris.accio.core.infra.uploader.local.LocalUploaderModule
import fr.cnrs.liris.common.flags.{Flag, FlagsParser}
import fr.cnrs.liris.privamov.ops.OpsModule

import scala.collection.mutable

case class AccioExecutorFlags(
  @Flag(name = "task_id") taskId: String,
  @Flag(name = "addr") addr: String,
  @Flag(name = "uploader.type") uploaderType: String,
  @Flag(name = "uploader.local.path") localUploaderPath: Path)

object AccioExecutorMain extends AccioExecutor

class AccioExecutor extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val parser = FlagsParser[AccioExecutorFlags]
    parser.parseAndExitUponError(args)
    val opts = parser.as[AccioExecutorFlags]

    val modules = mutable.ListBuffer[Module](GetterDownloaderModule, OpsModule, new ExecutorModule(opts.addr))
    opts.uploaderType match {
      case "local" => modules += new LocalUploaderModule(opts.localUploaderPath)
      case unknown => throw new IllegalArgumentException(s"Unknown uploader type: $unknown")
    }
    val injector = Guice.createInjector(modules: _*)

    val executor = injector.getInstance(classOf[TaskExecutor])
    Await.ready(executor.execute(TaskId(opts.taskId)))
  }
}