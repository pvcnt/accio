// Code has been translated from Bazel, subject to the following license:
/**
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

package fr.cnrs.liris.accio.tools.cli.terminal

import java.io.OutputStream

import com.google.common.base.Charsets

/**
 * A stream that writes to another one, emittig a prefix before every line
 * it emits. This stream will also add a newline for every flush; so it's not
 * useful for anything other than simple text data (e.g. log files). Here's
 * an example which demonstrates how an explicit flush or a flush caused by
 * a full buffer causes a newline to be added to the output.
 *
 * <code>
 * foo bar
 * baz ba[flush]ng
 * boo
 * </code>
 *
 * This results in this output being emitted:
 *
 * <code>
 * my prefix: foo bar
 * my prefix: ba
 * my prefix: ng
 * my prefix: boo
 * </code>
 */
final class LinePrefixingOutputStream(linePrefix: String, sink: OutputStream) extends LineFlushingOutputStream {
  private[this] val linePrefixBytes = linePrefix.getBytes(Charsets.UTF_8)

  override protected def flushingHook(): Unit = sink.synchronized {
    if (len == 0) {
      sink.flush()
    } else {
      val lastByte = buffer(len - 1)
      val lineIsIncomplete = lastByte != LineFlushingOutputStream.NewLine
      sink.write(linePrefixBytes)
      sink.write(buffer, 0, len)
      if (lineIsIncomplete) {
        sink.write(LineFlushingOutputStream.NewLine)
      }
      sink.flush()
      len = 0
    }
  }
}