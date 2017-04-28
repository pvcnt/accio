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

package fr.cnrs.liris.accio.agent.handler

import java.io.FileOutputStream
import java.nio.file.{Files, Path, Paths}

import com.google.common.io.Resources
import fr.cnrs.liris.accio.framework.filesystem.FileSystem
import fr.cnrs.liris.testing.{UnitSpec, WithTmpDirectory}

/**
 * Unit tests for [[ExecutorTaskExecutor]].
 */
class ExecutorTaskExecutorSpec extends UnitSpec with WithTmpDirectory {
  protected val executorUri: String = unpackExecutor().toAbsolutePath.toString

  /*protected def createExecutor: Scheduler = {
    val conf = new ExecutorTaskExecutor(tmpDir.resolve("workdir"), "0.0.0.0:12345", executorUri, None, Seq.empty)
    new LocalScheduler(MockFileSystem, NullStatsReceiver, conf)
  }

  protected def isRunning(key: String): Boolean = {
    ("ps -e -o args" #| "grep fr.cnrs.liris.accio.executor.AccioExecutorMain" #| s"grep $key" #| "grep -v grep").!  == 0
  }

  it should "schedule a job" in {
    val scheduler = createScheduler
    try {
      val job = Task(
        TaskId("1234"),
        RunId("run_id"),
        "NodeName",
        OpPayload("MyOp", 1234L, Map.empty, CacheKey("cache_key")),
        System.currentTimeMillis(),
        TaskState(TaskState.Waiting),
        Resource(1, 128, 0))
      val key = scheduler.submit(job)
      Thread.sleep(1000)
      isRunning(key) shouldBe true
    } finally {
      scheduler.close()
    }
  }

  it should "kill a running task" in {
    val scheduler = createScheduler
    try {
      val job = Task(
        TaskId("1234"),
        RunId("run_id"),
        "NodeName",
        OpPayload("MyOp", 1234L, Map.empty, CacheKey("cache_key")),
        System.currentTimeMillis(),
        TaskState(TaskState.Running, startedAt = Some(System.currentTimeMillis()), heartbeatAt = Some(System.currentTimeMillis())),
        Resource(1, 128, 0))
      val key = scheduler.submit(job)
      Thread.sleep(1000)
      scheduler.kill(key)
      isRunning(key) shouldBe false
    } finally {
      scheduler.close()
    }
  }*/

  private def unpackExecutor(): Path = {
    val path = Files.createTempFile("SchedulerSpec-", ".jar")
    val fos = new FileOutputStream(path.toFile)
    try {
      Resources.copy(Resources.getResource(s"fr/cnrs/liris/accio/agent/handler/accio-dummy-executor.jar"), fos)
    } finally {
      fos.close()
    }
    path
  }
}

private object MockFileSystem extends FileSystem {
  override def read(src: String, dst: Path): Unit = {
    Files.createDirectories(dst.getParent)
    Files.createSymbolicLink(dst, Paths.get(src))
  }

  override def write(src: Path, filename: String): String = throw new NotImplementedError

  override def delete(filename: String): Unit = throw new NotImplementedError
}