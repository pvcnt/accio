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

package fr.cnrs.liris.accio.filesystem.s3

import com.google.inject.Provides
import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.filesystem.FileSystem
import io.minio.MinioClient
import net.codingwell.scalaguice.ScalaMapBinder

/**
 * Guice module provisioning an S3 filesystem.
 */
object S3FileSystemModule extends TwitterModule {
  private val uriFlag = flag("filesystem.s3.uri", "https://s3.amazonaws.com", "URI to S3 server")
  private val bucketFlag = flag("filesystem.s3.bucket", "accio", "Bucket name")
  private val accessKeyFlag = flag[String]("filesystem.s3.access_key", "Access key with write access")
  private val privateKeyFlag = flag[String]("filesystem.s3.private_key", "Private key with write access")

  override protected def configure(): Unit = {
    val fileSystems = ScalaMapBinder.newMapBinder[String, FileSystem](binder)
    fileSystems.addBinding("s3").to[S3FileSystem]
  }

  @Provides
  def providesFileSystem: S3FileSystem = {
    val client = new MinioClient(uriFlag(), accessKeyFlag(), privateKeyFlag())
    new S3FileSystem(client, uriFlag(), bucketFlag())
  }
}