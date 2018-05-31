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
// https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/io/RecordingOutErr.java
package fr.cnrs.liris.infra.cli.io

import java.io.{ByteArrayOutputStream, UnsupportedEncodingException}

/**
 * An implementation of [[OutErr]] that captures all out / err output and
 * makes it available as ISO-8859-1 strings. Useful for implementing test
 * cases that assert particular output.
 */
final class RecordingOutErr(
  override val out: ByteArrayOutputStream,
  override val err: ByteArrayOutputStream)
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