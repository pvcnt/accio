/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.cli

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.file.Path

/**
 * Reporter is an abstraction of the standard output of CLI applications.
 */
trait Reporter {
  /**
   * Write a string to the standard output.
   *
   * @param str A string to write
   */
  def write(str: String): Unit

  /**
   * Write a string following by a line return to the standard output.
   *
   * @param str A string to write
   */
  def writeln(str: String = ""): Unit

  /**
   * Effectively write any pending change.
   */
  def flush(): Unit

  /**
   * Close the reporter and does not allow any further write.
   */
  def close(): Unit

  protected def format(str: String, useColors: Boolean): String = {
    if (useColors) {
      var formattedString = str
      for ((tag, color) <- Reporter.tags) {
        formattedString = formattedString.replace(s"<$tag>", color).replace(s"</$tag>", Console.RESET)
      }
      formattedString
    } else {
      str.replaceAll(s"</?(${Reporter.tags.keys.mkString("|")})>", "")
    }
  }
}

/**
 * Factory for [[Reporter]].
 */
object Reporter {
  private val tags = Map(
    "comment" -> Console.YELLOW,
    "info" -> Console.GREEN,
    "question" -> Console.CYAN_B,
    "error" -> Console.RED)

  def apply(out: PrintStream, useColors: Boolean = true): Reporter =
    new StreamReporter(out, useColors)

  def apply(path: Path): Reporter = {
    val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path.toFile, true)))
    new StreamReporter(out, useColors = false)
  }

  def apply(reporters: Reporter*): Reporter = new ComposedReporter(reporters)
}

/**
 * Default reporter writing in a stream.
 *
 * @param out       Output stream
 * @param useColors True if we are allowed to use colors, false otherwise
 */
final class StreamReporter(out: PrintStream, useColors: Boolean) extends Reporter {
  override def write(str: String): Unit = out.print(format(str, useColors))

  override def writeln(str: String): Unit = out.println(format(str, useColors))

  override def flush(): Unit = out.flush()

  override def close(): Unit = {}
}

/**
 * A reporter delegating the processing to multiple other reporters.
 *
 * @param reporters A list of reporters to delegate the processing to
 */
final class ComposedReporter(reporters: Seq[Reporter]) extends Reporter {
  override def write(str: String): Unit = reporters.foreach(_.write(str))

  override def writeln(str: String): Unit = reporters.foreach(_.writeln(str))

  override def flush(): Unit = reporters.foreach(_.flush())

  override def close(): Unit = reporters.foreach(_.close())
}