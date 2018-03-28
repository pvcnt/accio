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

package fr.cnrs.liris.accio.storage

import fr.cnrs.liris.accio.api.thrift._

/**
 * Common unit tests for all [[RunStore.Mutable]] implementations providing memoization
 * capabilities, ensuring they all have a consistent behavior.
 */
private[storage] trait RunStoreSpecWithMemoization {
  this: RunStoreSpec =>

  private val foobarRunWithNodes = foobarRun.copy(state = foobarRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some(CacheKey("MyFooCacheKey")), result = Some(foobarResults("FooNode"))),
    NodeStatus(name = "BarNode", status = TaskState.Success, cacheKey = Some(CacheKey("MyBarCacheKey")), result = Some(foobarResults("BarNode")))
  )))
  private val fooRunWithNodes = fooRun.copy(state = fooRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some(CacheKey("YourFooCacheKey")), result = Some(fooResults("FooNode"))))))

  it should "memoize artifacts" in {
    storage.write { stores =>
      stores.runs.save(foobarRunWithNodes)
      stores.runs.save(fooRunWithNodes)
    }
    storage.read { stores =>
      stores.runs.get(CacheKey("MyFooCacheKey")) shouldBe Some(foobarResults("FooNode"))
      stores.runs.get(CacheKey("MyBarCacheKey")) shouldBe Some(foobarResults("BarNode"))
      stores.runs.get(CacheKey("YourFooCacheKey")) shouldBe Some(fooResults("FooNode"))
      stores.runs.get(CacheKey("UnknownKey")) shouldBe None
    }
  }
}