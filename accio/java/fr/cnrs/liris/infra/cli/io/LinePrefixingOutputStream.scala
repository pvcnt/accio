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

// Code has been translated from Bazel, subject to the Apache License, Version 2.0.
// https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/io/LinePrefixingOutputStream.java

package fr.cnrs.liris.infra.cli.io

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
final class LinePrefixingOutputStream(linePrefix: String, sink: OutputStream)
  extends LineFlushingOutputStream {

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