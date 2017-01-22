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

package fr.cnrs.liris.accio.core.service

import fr.cnrs.liris.accio.core.domain._

import scala.collection.mutable

case class Job(taskId: TaskId, runId: RunId, nodeName: String, payload: OpPayload, resource: Resource)

trait Scheduler {
  def submit(job: Job): String

  def kill(key: String): Unit

  def stop(): Unit

  protected def createCommandLine(job: Job, executorPath: String, args: Seq[String], javaHome: Option[String] = None): Seq[String] = {
    val javaBinary = javaHome.map(home => s"$home/bin/java").getOrElse("/usr/bin/java")
    val cmd = mutable.ListBuffer.empty[String]
    cmd += javaBinary
    cmd += "-cp"
    cmd += executorPath
    cmd += s"-Xmx${job.resource.ramMb}M"
    cmd += "fr.cnrs.liris.accio.executor.AccioExecutorMain"
    cmd ++= args
    cmd += job.taskId.value
  }
}