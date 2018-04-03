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
import fr.cnrs.liris.accio.api.thrift.Task
import fr.cnrs.liris.common.scrooge.BinaryScroogeSerializer
import fr.cnrs.liris.locapriv.install.OpsModule

object AccioExecutorMain extends AccioExecutor

/**
 * Accio executor.
 */
class AccioExecutor {
  def main(args: Array[String]): Unit = {
    require(args.length >= 2, "There should be at least two arguments")
    val injector = Injector(Guice.createInjector(OpsModule))
    val executor = injector.instance[TaskExecutor]

    val task = BinaryScroogeSerializer.fromString(args.head, Task)
    com.twitter.jvm.numProcs.let(task.resource.cpu) {
      executor.execute(task, Paths.get(args(1)))
    }
  }
}