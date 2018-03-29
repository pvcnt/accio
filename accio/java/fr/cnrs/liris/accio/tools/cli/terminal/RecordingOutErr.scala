// Code has been translated from Bazel, subject to the following license:
/**
 * Copyright 2014 The Bazel Authors. All rights reserved.
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

package fr.cnrs.liris.accio.tools.cli.terminal

import java.io.{ByteArrayOutputStream, UnsupportedEncodingException}

/**
 * An implementation of [[OutErr]] that captures all out / err output and
 * makes it available as ISO-8859-1 strings. Useful for implementing test
 * cases that assert particular output.
 */
class RecordingOutErr(override val out: ByteArrayOutputStream, override val err: ByteArrayOutputStream)
  extends OutErr(out, err) {

  /**
   * Reset the captured content; that is, reset the out / err buffers.
   */
  def reset() {
    out.reset()
    err.reset()
  }

  /**
   * Interpret the captured out content as an ISO-8859-1 encoded string.
   */
  def outAsLatin1: String = {
    try {
      out.toString("ISO-8859-1")
    } catch {
      case e: UnsupportedEncodingException => throw new AssertionError(e)
    }
  }

  /**
   * Interpret the captured err content as an ISO-8859-1 encoded string.
   */
  def errAsLatin1: String = {
    try {
      err.toString("ISO-8859-1")
    } catch {
      case e: UnsupportedEncodingException => throw new AssertionError(e)
    }
  }

  /**
   * Check whether any output has been recorded.
   */
  def hasRecordedOutput: Boolean = out.size > 0 || err.size > 0

  override def toString: String = {
    val outString = outAsLatin1
    val errString = errAsLatin1
    (if (outString.length > 0) s"stdout: $outString\n" else "") +
      (if (errString.length > 0) s"stderr: $errString" else "")
  }
}

object RecordingOutErr {
  def apply(): RecordingOutErr = new RecordingOutErr(new ByteArrayOutputStream(), new ByteArrayOutputStream())
}