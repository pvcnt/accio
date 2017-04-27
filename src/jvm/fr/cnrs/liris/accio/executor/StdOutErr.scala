/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.executor

import java.io.{ByteArrayOutputStream, IOException, OutputStream, PrintStream}

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Charsets
import fr.cnrs.liris.accio.framework.service.OutErr

/**
 * Helper allowing to record stdout and stderr.
 */
object StdOutErr extends OutErr {
  private[this] val stdoutBytes = new ByteArrayOutputStream
  private[this] val stderrBytes = new ByteArrayOutputStream

  /**
   * Start recording stdout and stderr. It does not suppress the console output. This method is *not* thread-safe.
   */
  def record(): Unit = {
    System.setOut(new PrintStream(new ComposedOutputStream(Seq(System.out, stdoutBytes))))
    System.setErr(new PrintStream(new ComposedOutputStream(Seq(System.err, stderrBytes))))
  }

  override def stdoutAsString: String = synchronized {
    val content = new String(stdoutBytes.toByteArray, Charsets.UTF_8)
    stdoutBytes.reset()
    content
  }

  override def stderrAsString: String = synchronized {
    val content = new String(stderrBytes.toByteArray, Charsets.UTF_8)
    stderrBytes.reset()
    content
  }
}

/**
 * Implementation of an output stream forwarding calls to multiple underlying output streams.
 *
 * @param streams Output streams to forward calls to.
 */
@VisibleForTesting
private[executor] class ComposedOutputStream(streams: Seq[OutputStream]) extends OutputStream {
  override def write(i: Int): Unit = streams.foreach(_.write(i))

  @throws[IOException]
  override def write(bytes: Array[Byte]): Unit = streams.foreach(_.write(bytes))

  @throws[IOException]
  override def write(bytes: Array[Byte], off: Int, len: Int): Unit = streams.foreach(_.write(bytes, off, len))

  @throws[IOException]
  override def flush(): Unit = streams.foreach(_.flush())

  @throws[IOException]
  override def close(): Unit = streams.foreach(_.close())
}