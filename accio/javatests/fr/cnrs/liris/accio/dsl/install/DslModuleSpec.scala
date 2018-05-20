/*
 * Accio is a platform to launch computer science experiments.
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

package fr.cnrs.liris.accio.dsl.install

import com.google.inject.Module
import com.twitter.inject.CreateTwitterInjector
import fr.cnrs.liris.accio.dsl.json.JsonWorkflowParser
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[DslModule]].
 */
class DslModuleSpec extends UnitSpec with CreateTwitterInjector {
  behavior of "DslModule"

  override protected def modules: Seq[Module] = Seq(DslModule)

  it should "provide a JSON DSL parser" in {
    val injector = createInjector()
    injector.instance[JsonWorkflowParser] shouldBe a[JsonWorkflowParser]
  }
}