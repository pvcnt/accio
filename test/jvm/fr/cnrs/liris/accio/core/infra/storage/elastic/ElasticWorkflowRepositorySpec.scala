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

package fr.cnrs.liris.accio.core.infra.storage.elastic

import java.util.concurrent.TimeUnit

import com.google.inject.Guice
import com.twitter.finatra.json.FinatraObjectMapper
import fr.cnrs.liris.accio.core.domain._
import AccioFinatraJacksonModule
import fr.cnrs.liris.accio.core.infra.storage.WorkflowRepositorySpec

import scala.concurrent.duration.Duration

/**
 * Unit tests of [[ElasticWorkflowRepository]].
 */
class ElasticWorkflowRepositorySpec extends WorkflowRepositorySpec with ElasticStorageSpec {
  private[this] var i = 0
  private[this] val injector = Guice.createInjector(AccioFinatraJacksonModule)

  override protected def createRepository: WorkflowRepository = {
    // The node is node teared down at each test, which means data persists. We use a different indice each time to
    // start from a clean slate at each test.
    i += 1
    new ElasticWorkflowRepository(injector.getInstance(classOf[FinatraObjectMapper]), client, s"accio$i", Duration.create(10, TimeUnit.SECONDS))
  }

  override protected def refreshBeforeSearch(): Unit = {
    refreshAll()
  }

  behavior of "ElasticWorkflowRepository"
}