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

package fr.cnrs.liris.common.io

import java.io.OutputStream

/**
 * This stream maintains a buffer, which it flushes upon encountering bytes that might be new line characters.
 * This stream implements [[close()]] as [[flush()]].
 */
abstract class LineFlushingOutputStream extends OutputStream {
  /**
   * The buffer containing the characters that have not been flushed yet.
   */
  protected val buffer: Array[Byte] = Array.ofDim[Byte](LineFlushingOutputStream.BufferLength)

  /**
   * The length of the buffer that's actually used.
   */
  protected var len = 0

  override def write(b: Array[Byte], off: Int, inlen: Int): Unit = synchronized {
    if (len == LineFlushingOutputStream.BufferLength) {
      flush()
    }
    var offset = off
    var length = inlen
    var charsInLine = 0
    while (length > charsInLine) {
      val sawNewline = b(offset + charsInLine) == LineFlushingOutputStream.NewLine
      charsInLine += 1
      if (sawNewline || len + charsInLine == LineFlushingOutputStream.BufferLength) {
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