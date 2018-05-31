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
// https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/io/OutErr.java

package fr.cnrs.liris.infra.cli.io

import java.io.{OutputStream, PrintStream, PrintWriter}

/**
 * A pair of output streams to be used for redirecting the output and error streams of a subprocess.
 */
class OutErr(val out: OutputStream, val err: OutputStream) {
  def close(): Unit = {
    out.close()
    if (out != err) {
      err.close()
    }
  }

  /**
   * This method redirects [[System.out]]/[[System.err]] into this object. After calling this
   * method, writing to [[System.out]] or [[System.err]] will result in `"sys.out: " + message`
   * or `"sys.err: " + message` being written to the OutputStreams of this instance.
   *
   * Note: This method affects global variables.
   */
  def addSystemOutErrAsSource(): Unit = {
    System.setOut(new PrintStream(new LinePrefixingOutputStream("sys.out: ", out), false))
    System.setErr(new PrintStream(new LinePrefixingOutputStream("sys.err: ", err), false))
  }

  /**
   * Writes the specified string to the output stream, and flushes.
   */
  def printOut(str: String): Unit = {
    val writer = new PrintWriter(out, true)
    writer.print(str)
    writer.flush()
  }

  def printOutLn(str: String = ""): Unit = printOut(str + "\n")

  /**
   * Writes the specified string to the error stream, and flushes.
   */
  def printErr(str: String): Unit = {
    val writer = new PrintWriter(err, true)
    writer.print(str)
    writer.flush()
  }

  def printErrLn(str: String = ""): Unit = printErr(str + "\n")
}

object OutErr {
  val System = new OutErr(java.lang.System.out, java.lang.System.err)
}