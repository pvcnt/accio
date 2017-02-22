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

import com.google.inject.Provides
import fr.cnrs.liris.accio.core.uploader.Uploader
import fr.cnrs.liris.accio.core.uploader.util.ForUploader
import net.codingwell.scalaguice.ScalaModule
import net.schmizz.sshj.SSHClient

/**
 * Guice module provisioning an SCP uploader.
 *
 * @param config Configuration.
 */
class ScpUploaderModule(config: ScpUploaderConfig) extends ScalaModule {
  override protected def configure(): Unit = {
    bind[Uploader].to[ScpUploader]
    bind[ScpUploaderConfig].toInstance(config)
  }

  @Provides
  @ForUploader
  def providesSSHClient: SSHClient = {
    val client = new SSHClient
    client.useCompression()
    client
  }
}