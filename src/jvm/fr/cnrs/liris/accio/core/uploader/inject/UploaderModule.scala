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

package fr.cnrs.liris.accio.core.uploader.inject

import java.nio.file.Paths

import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.core.uploader.local.{LocalUploaderConfig, LocalUploaderModule}
import fr.cnrs.liris.accio.core.uploader.s3.{S3UploaderConfig, S3UploaderModule}
import fr.cnrs.liris.accio.core.uploader.scp.{ScpUploaderConfig, ScpUploaderModule}

/**
 * Guice module provisioning the [[fr.cnrs.liris.accio.core.uploader.Uploader]] service.
 */
object UploaderModule extends TwitterModule {
  private val uploaderFlag = flag("uploader.type", "local", "Uploader type")

  // Local uploader configuration.
  private val localPathFlag = flag[String]("uploader.local.path", "Path where to store files")

  // SCP uploader configuration.
  private val scpHostFlag = flag[String]("uploader.scp.host", "Host")
  private val scpPortFlag = flag("uploader.scp.port", 22, "SSH port")
  private val scpUserFlag = flag[String]("uploader.scp.user", "SSH username")
  private val scpPathFlag = flag[String]("uploader.scp.path", "Path on remote host where to store files, either absolute or relative to home directory")

  // S3 uploader configuration.
  private val s3UriFlag = flag("uploader.s3.uri", "https://s3.amazonaws.com", "URI to S3 server")
  private val s3BucketFlag = flag("uploader.s3.bucket", "accio", "Bucket name")
  private val s3AccessKeyFlag = flag[String]("uploader.s3.access_key", "Access key with write access")
  private val s3PrivateKeyFlag = flag[String]("uploader.s3.private_key", "Private key with write access")

  // Flags that will be forwarded "as-is" when invoking the executor.
  val executorPassthroughFlags = Seq(
    uploaderFlag,
    localPathFlag,
    scpHostFlag,
    scpPortFlag,
    scpUserFlag,
    scpPathFlag,
    s3UriFlag,
    s3BucketFlag,
    s3AccessKeyFlag,
    s3PrivateKeyFlag)

  protected override def configure(): Unit = {
    val module = uploaderFlag() match {
      case "local" => new LocalUploaderModule(LocalUploaderConfig(Paths.get(localPathFlag())))
      case "scp" =>
        var path = scpPathFlag()
        if (path.nonEmpty && path.endsWith("/")) {
          path += "/"
        }
        val config = ScpUploaderConfig(scpHostFlag(), scpPortFlag(), scpUserFlag(), path)
        new ScpUploaderModule(config)
      case "s3" =>
        val config = S3UploaderConfig(s3UriFlag(), s3BucketFlag(), s3AccessKeyFlag(), s3PrivateKeyFlag())
        new S3UploaderModule(config)
      case unknown => throw new IllegalArgumentException(s"Unknown uploader type: $unknown")
    }
    install(module)
  }
}