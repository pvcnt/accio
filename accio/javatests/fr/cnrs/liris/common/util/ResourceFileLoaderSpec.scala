// Comes from the Bazel project, subject to the following license:
/*
 * Copyright 2014 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package fr.cnrs.liris.common.util

import java.io.IOException

import fr.cnrs.liris.testing.UnitSpec

/**
 * Unit tests for [[ResourceFileLoader]].
 */
class ResourceFileLoaderSpec extends UnitSpec {
  behavior of "ResourceFileLoader"

  it should "load a resource file" in {
    val message = ResourceFileLoader.loadResource(classOf[ResourceFileLoaderSpec], "ResourceFileLoaderSpec.message")
    message shouldBe "Hello, world."
  }

  it should "detect an non-existant resource file" in {
    val expected = intercept[IOException] {
      ResourceFileLoader.loadResource(classOf[ResourceFileLoaderSpec], "does_not_exist.txt")
    }
    expected.getMessage shouldBe "does_not_exist.txt not found."
  }
}