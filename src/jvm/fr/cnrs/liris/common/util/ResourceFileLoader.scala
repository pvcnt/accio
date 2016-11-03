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

import com.google.common.base.Charsets
import com.google.common.io.ByteStreams

/**
 * A little utility to load resources (property files) from jars or
 * the classpath. Recommended for longer texts that do not fit nicely into
 * a piece of Java code - e.g. a template for a lengthy email.
 */
object ResourceFileLoader {
  /**
   * Loads a text resource that is located in a directory on the Java classpath that
   * corresponds to the package of <code>relativeToClass</code> using UTF8 encoding.
   * E.g.
   * <code>loadResource(Class.forName("com.google.foo.Foo", "bar.txt"))</code>
   * will look for <code>com/google/foo/bar.txt</code> in the classpath.
   */
  @throws[IOException]
  def loadResource(relativeToClass: Class[_], resourceName: String): String = {
    val loader = relativeToClass.getClassLoader
    // TODO: use relativeToClass.getPackage().getName().
    val className = relativeToClass.getName
    val packageName = className.substring(0, className.lastIndexOf('.'))
    val path = packageName.replace('.', '/')
    val resource = path + '/' + resourceName
    val stream = loader.getResourceAsStream(resource)
    if (stream == null) {
      throw new IOException(s"$resourceName not found.")
    }
    try {
      new String(ByteStreams.toByteArray(stream), Charsets.UTF_8)
    } finally {
      stream.close()
    }
  }
}