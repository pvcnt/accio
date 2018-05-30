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

package fr.cnrs.liris.locapriv.ops

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import fr.cnrs.liris.accio.sdk.OpContext
import fr.cnrs.liris.locapriv.domain.{Event, Poi}
import fr.cnrs.liris.lumos.domain.RemoteFile
import fr.cnrs.liris.sparkle.SparkleEnv
import fr.cnrs.liris.testing.CreateTmpDirectory
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

/**
 * Trait facilitating testing operators.
 */
private[ops] trait ScalaOperatorSpec extends BeforeAndAfterEach with CreateTmpDirectory {
  this: FlatSpec =>

  private val idx = new AtomicInteger(0)
  protected var env: SparkleEnv = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    env = new SparkleEnv(1)
  }

  override protected def afterEach(): Unit = {
    env.stop()
    env = null
    super.afterEach()
  }

  protected final def ctx: OpContext = {
    val workDir = tmpDir.resolve(s"tmp-${idx.getAndIncrement()}")
    Files.createDirectories(workDir)
    // This seed makes tests of unstable operators to pass for now. Be careful is you modify it!!
    new OpContext(Some(-7590331047132310476L), workDir)
  }

  protected final def writeTraces(data: Event*): RemoteFile = {
    val uri = s"file://$tmpDir/tmp-${idx.getAndIncrement()}"
    env.parallelize(data).write.csv(uri)
    RemoteFile(uri, Some("text/csv"))
  }

  protected final def writePois(data: Seq[Poi]): RemoteFile = {
    val uri = s"file://$tmpDir/tmp-${idx.getAndIncrement()}"
    env.parallelize(data).write.csv(uri)
    RemoteFile(uri, Some("text/csv"))
  }
}