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

import java.net.InetAddress
import java.nio.file.{Path, Paths}

import com.google.inject.Guice
import com.twitter.util.Await
import com.typesafe.scalalogging.LazyLogging
import fr.cnrs.liris.accio.core.infrastructure.uploader.local.{LocalUploader, LocalUploaderConfig, LocalUploaderModule}
import fr.cnrs.liris.accio.core.infrastructure.{Config, ConfigModule}
import fr.cnrs.liris.common.flags.inject.FlagsModule
import fr.cnrs.liris.common.flags.{Flag, FlagsParser}
import fr.cnrs.liris.privamov.ops.OpsModule

case class AccioExecutorFlags(
  @Flag(name = "id") taskId: String,
  @Flag(name = "tracker_addr") taskTrackerAddr: String,
  @Flag(name = "hostname") hostname: String = InetAddress.getLocalHost.getHostName,
  @Flag(name = "workdir") workDir: Path = Paths.get("."))

object AccioExecutorMain extends AccioExecutor

class AccioExecutor extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val parser = FlagsParser[AccioExecutorFlags]
    parser.parseAndExitUponError(args)
    val opts = parser.as[AccioExecutorFlags]

    val configs = Seq(
      Config(classOf[LocalUploader], LocalUploaderConfig(opts.workDir.resolve("uploads")))
    )

    val injector = Guice.createInjector(
      FlagsModule(parser),
      ConfigModule(configs: _*),
      ExecutorModule,
      OpsModule,
      LocalUploaderModule)

    val controller = injector.getInstance(classOf[ExecutorController])
    Await.ready(controller.execute(opts))
  }
}