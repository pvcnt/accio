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

package fr.cnrs.liris.accio.core.storage.inject

import java.nio.file.Path

import com.google.inject.{AbstractModule, Provides, Singleton}
import fr.cnrs.liris.accio.core.storage.{MutableRunRepository, MutableWorkflowRepository}
import fr.cnrs.liris.accio.core.storage.local.{LocalRunRepository, LocalWorkflowRepository}
import net.codingwell.scalaguice.ScalaModule

/**
 * Local storage configuration.
 *
 * @param path Root directory under which to store files.
 */
case class LocalStorageConfig(path: Path)

/**
 * Guice module provisioning repositories with local storage.
 *
 * @param config Configuration.
 */
final class LocalStorageModule(config: LocalStorageConfig) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Singleton
  @Provides
  def providesRunRepository: MutableRunRepository = {
    // Repository must be a singleton, otherwise the locking mechanism won't work.
    new LocalRunRepository(config.path.resolve("runs"))
  }

  @Singleton
  @Provides
  def providesWorkflowRepository: MutableWorkflowRepository = {
    // Repository must be a singleton, otherwise the locking mechanism won't work.
    new LocalWorkflowRepository(config.path.resolve("workflows"))
  }
}