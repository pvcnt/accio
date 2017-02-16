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

package fr.cnrs.liris.accio.core.scheduler.local

import com.twitter.finagle.stats.NullStatsReceiver
import fr.cnrs.liris.accio.core.downloader.NullDownloader
import fr.cnrs.liris.accio.core.scheduler.{Scheduler, SchedulerSpec}
import fr.cnrs.liris.testing.WithTmpDirectory
import org.scalatest.BeforeAndAfterEach

import scala.sys.process._

/**
 * Unit tests for [[LocalScheduler]].
 */
class LocalSchedulerSpec extends SchedulerSpec with BeforeAndAfterEach with WithTmpDirectory {
  behavior of "LocalScheduler"

  override protected def createScheduler: Scheduler = {
    val conf = LocalSchedulerConfig(tmpDir.resolve("workdir"), "0.0.0.0:12345", executorUri, None, Seq.empty)
    new LocalScheduler(NullDownloader, NullStatsReceiver).initialize(conf)
  }

  override protected def isRunning(key: String): Boolean = {
    Thread.sleep(1000)
    ("ps -e -o args" #| s"grep java").lineStream.foreach(println)
    ("ps -e -o args" #| s"grep $executorUri" #| s"grep $key" #| "grep -v grep").! == 0
  }
}