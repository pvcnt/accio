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

package fr.cnrs.liris.accio.core.infra.uploader.scp

import java.io.IOException
import java.nio.file.Path

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.service.Uploader
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.method.{AuthMethod, AuthPublickey}
import net.schmizz.sshj.xfer.FileSystemFile

class ScpUploader(user: String, host: String, path: String, publicKey: Option[String])
  extends Uploader with StrictLogging {

  private[this] lazy val ssh = {
    val ssh = new SSHClient
    ssh.connect(host)
    ssh.auth(user, createAuthPublicKey(ssh, publicKey): _*)
    ssh.useCompression()
    ssh
  }

  override def upload(src: Path, key: String): String = {
    ssh.newSCPFileTransfer().upload(new FileSystemFile(src.toAbsolutePath.toString), s"$path/$key")
    s"$user@$host/$path/$key"
  }

  def close(): Unit = {
    ssh.disconnect()
  }

  private def createAuthPublicKey(ssh: SSHClient, publicKey: Option[String]): Seq[AuthMethod] = {
    val base = sys.props("user.home") + "/.ssh/"
    val paths = Seq(base + "id_rsa", base + "id_dsa", base + "id_ed25519", base + "id_ecdsa") ++ publicKey.toSeq
    paths.flatMap { path =>
      try {
        logger.debug(s"Attempting to load key from: $path")
        Some(ssh.loadKeys(path))
      } catch {
        case e: IOException =>
          logger.info(s"Could not load keys from $path due to: ${e.getMessage}")
          None
      }
    }.map(keyProvider => new AuthPublickey(keyProvider))
  }
}