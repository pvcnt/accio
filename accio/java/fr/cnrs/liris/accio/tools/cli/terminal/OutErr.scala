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

import java.io.{IOException, OutputStream, PrintStream, PrintWriter}

/**
 * A pair of output streams to be used for redirecting the output and error streams of a subprocess.
 */
class OutErr(val out: OutputStream, val err: OutputStream) {
  @throws[IOException]
  def close(): Unit = {
    out.close()
    if (out != err) {
      err.close()
    }
  }

  /**
   * This method redirects [[System.out]]/[[System.err]] into this object. After calling this method, writing to
   * [[System.out]] or [[System.err]] will result in `"System.out: " + message` or `"System.err: " + message`
   * being written to the OutputStreams of this instance.
   *
   * Note: This method affects global variables.
   */
  def addSystemOutErrAsSource(): Unit = {
    System.setOut(new PrintStream(new LinePrefixingOutputStream("System.out: ", out), false))
    System.setErr(new PrintStream(new LinePrefixingOutputStream("System.err: ", err), false))
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