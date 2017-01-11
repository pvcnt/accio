/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.common.getter

import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.{ScalaMapBinder, ScalaModule, ScalaMultibinder}

object GetterModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val detectors = ScalaMultibinder.newSetBinder[Detector](binder)
    detectors.addBinding.to[FileDetector]
    detectors.addBinding.to[GitHubDetector]
    detectors.addBinding.to[S3Detector]

    val decompressors = ScalaMultibinder.newSetBinder[Decompressor](binder)
    decompressors.addBinding.to[TarDecompressor]
    decompressors.addBinding.to[GzipDecompressor]
    decompressors.addBinding.to[Bzip2Decompressor]
    decompressors.addBinding.to[ZipDecompressor]
    decompressors.addBinding.to[TarGzipDecompressor]
    decompressors.addBinding.to[TarBzip2Decompressor]

    val getters = ScalaMultibinder.newSetBinder[Getter](binder)
    getters.addBinding.to[FileGetter]
    getters.addBinding.to[HttpGetter]
  }

  @Provides
  def providesFileGetter: FileGetter = {
    new FileGetter(copy = false)
  }

  @Provides
  def providesClient(detectors: Set[Detector], getters: Set[Getter], decompressors: Set[Decompressor]): DownloadClient = {
    new DownloadClient(detectors, getters, decompressors, guessPwd = true)
  }
}