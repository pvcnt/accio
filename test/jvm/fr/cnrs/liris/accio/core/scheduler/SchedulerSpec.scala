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

package fr.cnrs.liris.accio.core.scheduler

import java.io.FileOutputStream
import java.nio.file.{Files, Path}

import com.google.common.io.Resources
import fr.cnrs.liris.accio.core.domain._
import fr.cnrs.liris.testing.UnitSpec

/**
 * Common unit tests for all [[Scheduler]] implementations, ensuring they all have consistent behavior.
 */
private[scheduler] abstract class SchedulerSpec extends UnitSpec {
  protected val executorUri: String = unpackExecutor().toAbsolutePath.toString

  protected def createScheduler: Scheduler

  protected def isRunning(key: String): Boolean

  it should "schedule a job" in {
    val scheduler = createScheduler
    try {
      val job = Job(TaskId("1234"), RunId("run_id"), "NodeName", OpPayload("MyOp", 1234L, Map.empty, CacheKey("cache_key")), Resource(1, 128, 0))
      val key = scheduler.submit(job)
      isRunning(key) shouldBe true
    } finally {
      scheduler.close()
    }
  }

  it should "kill a running task" in {
    val scheduler = createScheduler
    try {
      val job = Job(TaskId("1234"), RunId("run_id"), "NodeName", OpPayload("MyOp", 1234L, Map.empty, CacheKey("cache_key")), Resource(0, 0, 0))
      val key = scheduler.submit(job)
      scheduler.kill(key)
      isRunning(key) shouldBe false
    } finally {
      scheduler.close()
    }
  }

  private def unpackExecutor(): Path = {
    val path = Files.createTempFile("SchedulerSpec-", ".jar")
    val fos = new FileOutputStream(path.toFile)
    try {
      Resources.copy(Resources.getResource(s"fr/cnrs/liris/accio/core/scheduler/accio-dummy-executor.jar"), fos)
    } finally {
      fos.close()
    }
    path
  }
}
