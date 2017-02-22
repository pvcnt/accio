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

package fr.cnrs.liris.accio.core.filesystem.s3

import com.google.inject.Provides
import fr.cnrs.liris.accio.core.filesystem.{FileSystem, InjectFileSystem}
import io.minio.MinioClient
import net.codingwell.scalaguice.{ScalaMapBinder, ScalaModule}

/**
 * Guice module provisioning an S3 filesystem.
 *
 * @param config Configuration.
 */
final class S3FileSystemModule(config: S3FileSystemConfig) extends ScalaModule {
  override protected def configure(): Unit = {
    val fileSystems = ScalaMapBinder.newMapBinder[String, FileSystem](binder)
    fileSystems.addBinding("s3").to[S3FileSystem]
    bind[S3FileSystemConfig].toInstance(config)
  }

  @Provides
  @InjectFileSystem
  def providesS3Client: MinioClient = new MinioClient(config.uri, config.accessKey, config.secretKey)
}