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

package fr.cnrs.liris.accio.core.filesystem.inject

import java.nio.file.Paths

import com.google.inject.Module
import com.twitter.inject.{Injector, TwitterModule}
import fr.cnrs.liris.accio.core.filesystem.FileSystem
import fr.cnrs.liris.accio.core.filesystem.posix.{PosixFileSystemConfig, PosixFileSystemModule}
import fr.cnrs.liris.accio.core.filesystem.s3.{S3FileSystemConfig, S3FileSystemModule}

import scala.collection.mutable

/**
 * Guice module provisioning the filesystem service.
 */
object FileSystemModule extends TwitterModule {
  // POSIX filesystem configuration.
  private val posixEnabledFlag = flag[Boolean]("fs.posix.enabled", false, "Enable POSIX filesystem")
  private val posixPathFlag = flag[String]("fs.posix.path", "Path where to store files")
  private val posixSymlinkFlag = flag("fs.posix.symlink", true, "Path where to symlink files")

  // S3 filesystem configuration.
  private val s3EnabledFlag = flag("fs.s3.enabled", false, "Enable S3 filesystem")
  private val s3UriFlag = flag("fs.s3.uri", "https://s3.amazonaws.com", "URI to S3 server")
  private val s3BucketFlag = flag("fs.s3.bucket", "accio", "Bucket name")
  private val s3AccessKeyFlag = flag[String]("fs.s3.access_key", "Access key with write access")
  private val s3PrivateKeyFlag = flag[String]("fs.s3.private_key", "Private key with write access")

  // Flags that will be forwarded "as-is" when invoking the executor.
  val executorPassthroughFlags = Seq(
    posixEnabledFlag,
    posixPathFlag,
    posixSymlinkFlag,
    s3EnabledFlag,
    s3UriFlag,
    s3BucketFlag,
    s3AccessKeyFlag,
    s3PrivateKeyFlag)

  protected override def configure(): Unit = {
    val modules = mutable.Set.empty[Module]
    if (posixEnabledFlag()) {
      val config = PosixFileSystemConfig(Paths.get(posixPathFlag()), posixSymlinkFlag())
      modules += new PosixFileSystemModule(config)
    }
    if (s3EnabledFlag()) {
      val config = S3FileSystemConfig(s3UriFlag(), s3BucketFlag(), s3AccessKeyFlag(), s3PrivateKeyFlag())
      modules += new S3FileSystemModule(config)
    }
    if (modules.isEmpty) {
      logger.warn("No filesystem is provisioned")
    }
    modules.foreach(install)
    bind[FileSystem].to[PluginFileSystem]
  }

  override def singletonShutdown(injector: Injector): Unit = {
    injector.instance[FileSystem].close()
  }
}