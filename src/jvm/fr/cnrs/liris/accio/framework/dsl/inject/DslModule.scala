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

package fr.cnrs.liris.accio.framework.dsl.inject

import com.google.inject.{Exposed, Provides, Singleton}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.TwitterPrivateModule
import fr.cnrs.liris.accio.framework.dsl.{ObjectMapperFactory, RunParser, WorkflowParser}
import fr.cnrs.liris.accio.framework.service.{OpRegistry, RunFactory, WorkflowFactory}
import fr.cnrs.liris.accio.framework.storage.Storage

/**
 * Guice module provisioning DSL parsers.
 */
object DslModule extends TwitterPrivateModule {
  @Provides @Singleton
  def providesObjectMapper: FinatraObjectMapper = {
    new ObjectMapperFactory().create()
  }

  @Provides @Exposed
  def providesRunParser(mapper: FinatraObjectMapper, storage: Storage, factory: RunFactory): RunParser = {
    new RunParser(mapper, storage, factory)
  }

  @Provides @Exposed
  def providesWorkflowParser(mapper: FinatraObjectMapper, opRegistry: OpRegistry, factory: WorkflowFactory): WorkflowParser = {
    new WorkflowParser(mapper, opRegistry, factory)
  }
}