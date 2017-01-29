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

import fr.cnrs.liris.accio.core.uploader.Uploader
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.FileSystemFile

/**
 * Uploader using an SCP client to uploader files. Authentication is done by public key.
 *
 * @param client SSH client (should be in a disconnected state).
 * @param host   Host where to upload files.
 * @param user   Username.
 * @param path   Root path where to upload files.
 */
class ScpUploader(client: SSHClient, host: String, user: String, path: String) extends Uploader {
  override def upload(src: Path, key: String): String = {
    if (!client.isConnected) {
      client.connect(host)
      client.authPublickey(user)
      client.useCompression()
    }
    client.newSCPFileTransfer().upload(new FileSystemFile(src.toAbsolutePath.toString), s"$path/$key")
    s"$user@$host/$path/$key"
  }

  override def close(): Unit = {
    if (client.isConnected) {
      client.disconnect()
    }
  }
}