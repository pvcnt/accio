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

package fr.cnrs.liris.accio.core.infra.statemgr.local

import java.nio.file.Path

import com.google.inject.Provides
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.application.{Configurable, StateManager}
import net.codingwell.scalaguice.ScalaModule

/**
 * Local state manager configuration.
 *
 * @param rootDir Root directory under which data will be written.
 */
case class LocalStateMgrConfig(rootDir: Path)

/**
 * Guice module provisioning a local state manager.
 */
final class LocalStateMgrModule extends ScalaModule with Configurable[LocalStateMgrConfig] {
  override def configClass: Class[LocalStateMgrConfig] = classOf[LocalStateMgrConfig]

  override def configure(): Unit = {}

  @Provides
  def providesStateManager(mapper: FinatraObjectMapper): StateManager = {
    new LocalStateMgr(mapper, config.rootDir)
  }
}
