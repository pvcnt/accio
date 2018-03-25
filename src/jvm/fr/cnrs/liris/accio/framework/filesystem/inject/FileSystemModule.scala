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

package fr.cnrs.liris.accio.framework.filesystem.inject

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.framework.filesystem.FileSystem
import fr.cnrs.liris.accio.framework.filesystem.posix.PosixFileSystemModule
import fr.cnrs.liris.accio.framework.filesystem.s3.S3FileSystemModule

/**
 * Guice module provisioning the filesystem service.
 */
object FileSystemModule extends TwitterModule {
  private[this] val typeFlag = flag[String]("filesystem.type", "Filesystem type(s)")

  // Flags that will be forwarded "as-is" when invoking the executor.
  def executorPassthroughFlags: Seq[Flag[_]] =
    Seq(typeFlag) ++ PosixFileSystemModule.executorPassthroughFlags ++ S3FileSystemModule.executorPassthroughFlags

  override def modules: Seq[Module] =
    typeFlag.get match {
      case Some(types) =>
        types.split(",").toSeq.map(_.trim.toLowerCase).map {
          case "posix" => PosixFileSystemModule
          case "s3" => S3FileSystemModule
          case invalid => throw new IllegalArgumentException(s"Invalid filesystem type: $invalid")
        }
      case None =>
        // This only happen when this method is called the first time, during App's initialization.
        // We provide the entire list of all modules, making all flags available.
        Seq(PosixFileSystemModule, S3FileSystemModule)
    }

  protected override def configure(): Unit = {
    bind[FileSystem].to[PluginFileSystem]
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[FileSystem].close()
  }
}