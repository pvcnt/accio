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

import com.google.inject.Guice
import com.twitter.inject.Injector
import fr.cnrs.liris.accio.api.thrift.OpPayload
import fr.cnrs.liris.locapriv.install.OpsModule
import fr.cnrs.liris.util.scrooge.BinaryScroogeSerializer

object AccioExecutorMain extends AccioExecutor

/**
 * Accio executor.
 */
class AccioExecutor {
  def main(args: Array[String]): Unit = {
    require(args.length >= 2, "There should be at least two arguments")
    val injector = Injector(Guice.createInjector(OpsModule))
    val executor = injector.instance[TaskExecutor]

    val payload = BinaryScroogeSerializer.fromString(args.head, OpPayload)
    // The following line is here to trick the Sparkle executor in thinking there is less cores
    // than effectively available. This is used as a poor-man isolation system, when nothing more
    // sophisticated is in place, to prevent Sparkle from using all available cores.
    val exitCode = com.twitter.jvm.numProcs.let(payload.resources.cpus) {
      executor.execute(payload, Paths.get(args(1)))
    }

    // The exit code is returned by the executor, because it will be captured by the scheduler
    // later on.
    sys.exit(exitCode)
  }
}