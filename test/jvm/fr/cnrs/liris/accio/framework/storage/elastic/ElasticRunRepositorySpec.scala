/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.accio.framework.storage.elastic

import java.util.concurrent.atomic.AtomicInteger

import com.twitter.util.Duration
import fr.cnrs.liris.accio.framework.storage.{MutableRunRepository, RunRepositorySpecWithMemoization}

/**
 * Unit tests of [[ElasticRunRepository]].
 */
class ElasticRunRepositorySpec extends RunRepositorySpecWithMemoization with ElasticRepositorySpec {
  behavior of "ElasticRunRepository"

  private[this] val i = new AtomicInteger

  override protected def createRepository: MutableRunRepository = {
    // The node is node teared down at each test, which means data persists. We use a different indice each time to
    // start from a clean slate at each test.
    val mapper = new ObjectMapperFactory().create()
    new ElasticRunRepository(mapper, client, s"accio${i.incrementAndGet}", Duration.Top)
  }

  override protected def refreshBeforeSearch(): Unit = refreshAll()
}