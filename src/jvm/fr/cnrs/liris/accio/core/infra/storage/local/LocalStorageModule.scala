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

package fr.cnrs.liris.accio.core.infra.storage.local

import java.nio.file.Path

import com.google.inject.{AbstractModule, Provides, Singleton}
import fr.cnrs.liris.accio.core.domain.{RunRepository, WorkflowRepository}
import net.codingwell.scalaguice.ScalaModule

/**
 * Guice module provisioning repositories with local storage.
 *
 * Note: Repositories should be singletons, otherwise the locking mechanism won't work.
 *
 * @param path Root directory under which to store files.
 */
final class LocalStorageModule(path: Path) extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  @Singleton
  @Provides
  def providesRunRepository: RunRepository = new LocalRunRepository(path.resolve("runs"))

  @Singleton
  @Provides
  def providesWorkflowRepository: WorkflowRepository = new LocalWorkflowRepository(path.resolve("workflows"))
}