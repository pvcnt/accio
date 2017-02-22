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

import java.nio.file.Path

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.filesystem.InjectFileSystem
import fr.cnrs.liris.accio.core.filesystem.archive.{ArchivedFileSystem, TarGzipArchiveFormat}
import io.minio.MinioClient

/**
 * Uploader copying files on S3/Minio.
 *
 * @param client S3 client.
 * @param config Uploader configuration.
 */
@Singleton
final class S3FileSystem @Inject()(@InjectFileSystem client: MinioClient, config: S3FileSystemConfig)
  extends ArchivedFileSystem(TarGzipArchiveFormat) {

  override protected def doWrite(src: Path, key: String): String = {
    if (!client.bucketExists(config.bucket)) {
      client.makeBucket(config.bucket)
    }
    client.putObject(config.bucket, s"$key.tar.gz", src.toAbsolutePath.toString)
    s"${config.uri.stripSuffix("/")}/${config.bucket}/$key.tar.gz"
  }

  override protected def doRead(filename: String, dst: Path): Unit = {
    client.getObject(config.bucket, filename, dst.toAbsolutePath.toString)
  }

  override def delete(filename: String): Unit = {
    client.removeObject(config.bucket, filename)
  }
}