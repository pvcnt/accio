/*
 * Accio is a program whose purpose is to study location privacy.
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

package fr.cnrs.liris.accio.filesystem.posix

import java.nio.file.{Path, Paths}

import com.twitter.app.Flag
import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.filesystem.FileSystem
import net.codingwell.scalaguice.ScalaMapBinder

/**
 * Guice module provisioning a POSIX filesystem.
 */
object PosixFileSystemModule extends TwitterModule {
  private[this] val pathFlag = flag[String]("filesystem.posix.root", "Path where to store files")
  private[this] val symlinkFlag = flag("filesystem.posix.symlink", true, "Whether to symlink files")

  def executorPassthroughFlags: Seq[Flag[_]] = Seq(pathFlag, symlinkFlag)

  override protected def configure(): Unit = {
    val fileSystems = ScalaMapBinder.newMapBinder[String, FileSystem](binder)
    fileSystems.addBinding("posix").to[PosixFileSystem]

    bind[Path].annotatedWith[PosixPath].toInstance(Paths.get(pathFlag()))
    bind[Boolean].annotatedWith[PosixSymlink].toInstance(symlinkFlag())
  }
}