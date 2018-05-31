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
// https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/io/LineFlushingOutputStream.java

package fr.cnrs.liris.infra.cli.io

import java.io.OutputStream

/**
 * This stream maintains a buffer, which it flushes upon encountering bytes that might be new line
 * characters. This stream implements [[close()]] as [[flush()]].
 */
abstract class LineFlushingOutputStream extends OutputStream {

  import LineFlushingOutputStream._

  //The buffer containing the characters that have not been flushed yet.
  protected val buffer: Array[Byte] = Array.ofDim[Byte](BufferLength)
  // The length of the buffer that's actually used.
  protected var len = 0

  override def write(b: Array[Byte], off: Int, inlen: Int): Unit = synchronized {
    if (len == BufferLength) {
      flush()
    }
    var offset = off
    var length = inlen
    var charsInLine = 0
    while (length > charsInLine) {
      val sawNewline = b(offset + charsInLine) == NewLine
      charsInLine += 1
      if (sawNewline || len + charsInLine == BufferLength) {
        System.arraycopy(b, offset, buffer, len, charsInLine)
        len += charsInLine
        offset += charsInLine
        length -= charsInLine
        flush()
        charsInLine = 0
      }
    }
    System.arraycopy(b, offset, buffer, len, charsInLine)
    len += charsInLine
  }

  override def write(byteAsInt: Int): Unit = {
    val b = byteAsInt.toByte // make sure we work with bytes in comparisons
    write(Array(b), 0, 1)
  }

  /**
   * Close is implemented as [[flush()]]. Client code must close the
   * underlying output stream itself in case that's desired.
   */
  override def close(): Unit = synchronized {
    flush()
  }

  override final def flush(): Unit = synchronized {
    flushingHook() // The point of using a hook is to make it synchronized.
  }

  /**
   * The implementing class must define this method, which must at least flush
   * the bytes in `buffer[0] - buffer[len - 1]`, and reset `len = 0`.
   *
   * Don't forget to synchronize the implementation of this method on whatever
   * underlying object it writes to!
   */
  protected def flushingHook(): Unit
}

object LineFlushingOutputStream {
  private[io] val BufferLength = 8192
  private[io] val NewLine = '\n'.toByte
}