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

import java.nio.file.Path

import fr.cnrs.liris.accio.core.uploader.{Archiver, Uploader}
import fr.cnrs.liris.accio.core.util.Configurable
import io.minio.MinioClient

/**
 * Uploader copying files on S3/Minio.
 */
class S3Uploader extends Uploader with Archiver with Configurable[S3UploaderConfig] {
  private[this] lazy val client = new MinioClient(config.uri, config.accessKey, config.secretKey)

  override def configClass: Class[S3UploaderConfig] = classOf[S3UploaderConfig]

  override def upload(src: Path, key: String): String = {
    if (!client.bucketExists(config.bucket)) {
      client.makeBucket(config.bucket)
    }
    val tarGzFile = archiveAndCompress(src)
    client.putObject(config.bucket, s"$key.tar.gz", tarGzFile.toAbsolutePath.toString)
    s"s3::${config.uri.stripSuffix("/")}/${config.bucket}/$key.tar.gz"
  }
}