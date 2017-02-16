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

package fr.cnrs.liris.accio.core.storage.elastic

import com.google.inject.Guice
import com.twitter.util.Duration
import fr.cnrs.liris.accio.core.storage.MutableRunRepository
import fr.cnrs.liris.testing.UnitSpec

class ElasticStorageModuleSpec extends UnitSpec {
  behavior of "ElasticStorageModule"

  it should "provide a run repository" in {
    val module = new ElasticStorageModule(ElasticStorageConfig("foo:123", "accio", Duration.fromSeconds(5)))
    val injector = Guice.createInjector(module)
    //injector.getInstance(classOf[MutableRunRepository]) shouldBe a[MutableRunRepository]
  }
}
