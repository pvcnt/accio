/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
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
    "error" -> (Console.WHITE_B + Console.RED_B))

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