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

package fr.cnrs.liris.accio.core.downloader.inject

import fr.cnrs.liris.accio.core.downloader.Downloader
import fr.cnrs.liris.accio.core.downloader.getter.GetterDownloader
import fr.cnrs.liris.common.getter.GetterModule
import net.codingwell.scalaguice.ScalaModule

/**
 * Guice module provisioning a downloader using the common/getter module.
 */
object DownloaderModule extends ScalaModule {
  override def configure(): Unit = {
    install(GetterModule)
    bind[Downloader].to[GetterDownloader]
  }
}
