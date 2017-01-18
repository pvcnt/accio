/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

import com.twitter.inject.app.App
import com.twitter.util.Await
import fr.cnrs.liris.accio.core.domain.TaskId
import fr.cnrs.liris.privamov.ops.OpsModule

object AccioExecutorMain extends AccioExecutor

class AccioExecutor extends App {
  override protected def modules = Seq(ExecutorModule, OpsModule)

  override protected def run(): Unit = {
    require(args.length == 1, "You must provide a single task identifier as argument")
    val executor = injector.instance[TaskExecutor]
    Await.ready(executor.execute(TaskId(args.head)))
  }
}