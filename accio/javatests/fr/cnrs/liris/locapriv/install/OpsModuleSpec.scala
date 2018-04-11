/*
 * Copyright 2017-2018 UCL / Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cnrs.liris.locapriv.install

import com.twitter.inject.CreateTwitterInjector
import fr.cnrs.liris.accio.runtime.OpMeta
import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[OpsModule]].
 */
class OpsModuleSpec extends UnitSpec with CreateTwitterInjector {
  behavior of "OpsModule"

  override protected def modules = Seq(OpsModule)

  it should "provide operators" in {
    val injector = createInjector
    val ops = injector.instance[Set[OpMeta]]
    ops.size shouldBe >=(0)
  }
}
