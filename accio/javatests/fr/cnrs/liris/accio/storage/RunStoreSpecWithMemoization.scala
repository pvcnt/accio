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

import fr.cnrs.liris.accio.api.Values
import fr.cnrs.liris.accio.api.thrift._

import scala.collection.Map

/**
 * Common unit tests for all [[RunStore.Mutable]] implementations providing memoization
 * capabilities, ensuring they all have a consistent behavior.
 */
private[storage] trait RunStoreSpecWithMemoization {
  this: RunStoreSpec =>

  private val foobarResults = Map(
    "FooNode" -> OpResult(
      0,
      Set(Artifact("myint", Values.encodeInteger(42)), Artifact("mystr", Values.encodeString("foo str"))),
      Set(Metric("a", 1), Metric("b", 2))),
    "BarNode" -> OpResult(
      0,
      Set(Artifact("dbl", Values.encodeDouble(3.14))),
      Set(Metric("a", 12))))
  private val fooResults = Map("FooNode" -> OpResult(
    0,
    Set(Artifact("myint", Values.encodeInteger(44)), Artifact("mystr", Values.encodeString("str"))),
    Set(Metric("a", 3), Metric("b", 4))))
  private val foobarRunWithNodes = foobarRun.copy(state = foobarRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some("MyFooCacheKey"), result = Some(foobarResults("FooNode"))),
    NodeStatus(name = "BarNode", status = TaskState.Success, cacheKey = Some("MyBarCacheKey"), result = Some(foobarResults("BarNode")))
  )))
  private val fooRunWithNodes = fooRun.copy(state = fooRun.state.copy(nodes = Set(
    NodeStatus(name = "FooNode", status = TaskState.Success, cacheKey = Some("YourFooCacheKey"), result = Some(fooResults("FooNode"))))))

  it should "memoize artifacts" in {
    storage.write { stores =>
      stores.runs.save(foobarRunWithNodes)
      stores.runs.save(fooRunWithNodes)
    }
    storage.read { stores =>
      stores.runs.fetch("MyFooCacheKey") shouldBe Some(foobarResults("FooNode"))
      stores.runs.fetch("MyBarCacheKey") shouldBe Some(foobarResults("BarNode"))
      stores.runs.fetch("YourFooCacheKey") shouldBe Some(fooResults("FooNode"))
      stores.runs.fetch("UnknownKey") shouldBe None
    }
  }
}