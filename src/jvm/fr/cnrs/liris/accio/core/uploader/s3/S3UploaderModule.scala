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

package fr.cnrs.liris.accio.core.uploader.s3

import com.google.inject.{Provides, Singleton}
import fr.cnrs.liris.accio.core.uploader.Uploader
import fr.cnrs.liris.accio.core.uploader.util.ForUploader
import io.minio.MinioClient
import net.codingwell.scalaguice.ScalaModule

/**
 * Guice module provisioning an S3 uploader.
 *
 * @param config Configuration.
 */
final class S3UploaderModule(config: S3UploaderConfig) extends ScalaModule {
  override protected def configure(): Unit = {
    bind[Uploader].to[S3Uploader]
    bind[S3UploaderConfig].toInstance(config)
  }

  @Provides
  @ForUploader
  def providesS3Client: MinioClient = new MinioClient(config.uri, config.accessKey, config.secretKey)
}