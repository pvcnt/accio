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

package fr.cnrs.liris.accio.core.uploader.scp

import java.nio.file.Path

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.core.uploader.util.{Archiver, ForUploader}
import fr.cnrs.liris.accio.core.uploader.Uploader
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.FileSystemFile

/**
 * Uploader using an SCP client to uploader files. Authentication is done by public key.
 */
@Singleton
final class ScpUploader @Inject()(@ForUploader client: SSHClient, config: ScpUploaderConfig) extends Uploader with Archiver {
  override def upload(src: Path, key: String): String = {
    connect()
    val tarGzFile = archiveAndCompress(src)
    val path = s"${config.path}$key.tar.gz"
    client.newSCPFileTransfer().upload(new FileSystemFile(tarGzFile.toAbsolutePath.toString), path)
    s"scp::${config.user}@${config.host}:${config.port}/$path"
  }

  override def close(): Unit = {
    if (client.isConnected) {
      client.disconnect()
    }
  }

  private def connect() = {
    if (!client.isConnected) {
      client.connect(config.host, config.port)
      client.authPublickey(config.user)
    }
  }
}