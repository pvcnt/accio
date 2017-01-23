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

package fr.cnrs.liris.accio.core.infra.inject

import java.nio.file.Paths

import com.twitter.inject.TwitterModule
import fr.cnrs.liris.accio.core.infra.uploader.local.{LocalUploaderConfig, LocalUploaderModule}
import fr.cnrs.liris.accio.core.service._

/**
 * Guice module provisioning the [[Uploader]] service.
 */
object UploaderModule extends TwitterModule {
  private[inject] val uploaderFlag = flag("uploader.type", "local", "Uploader type")
  private[inject] val localUploaderPathFlag = flag[String]("uploader.local.path", "Path where to store files")

  protected override def configure(): Unit = {
    val module = uploaderFlag() match {
      case "local" => new LocalUploaderModule(LocalUploaderConfig(Paths.get(localUploaderPathFlag())))
      case unknown => throw new IllegalArgumentException(s"Unknown uploader type: $unknown")
    }
    install(module)
  }
}
