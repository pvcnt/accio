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

package fr.cnrs.liris.accio.docgen

import java.io.{BufferedOutputStream, FileOutputStream, OutputStream}
import java.nio.file.Path

import com.google.inject.{Guice, Inject}
import com.twitter.util.Stopwatch
import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.framework.{OpMeta, OpRegistry}
import fr.cnrs.liris.common.flags._
import fr.cnrs.liris.privamov.ops.OpsModule

object AccioDocgenMain extends AccioDocgen

case class AccioDocgenFlags(
  @Flag(name = "out", help = "File where to write generated documentation")
  out: Path,
  @Flag(name = "toc", help = "Whether to include a table of contents")
  toc: Boolean = true,
  @Flag(name = "layout")
  layout: String = "privacy",
  @Flag(name = "nav")
  nav: String = "privacy",
  @Flag(name = "title")
  title: String = "Operators library")

/**
 * Quick and dirty Markdown documentation generator for operators.
 */
class AccioDocgen extends StrictLogging {
  def main(args: Array[String]): Unit = {
    val elapsed = Stopwatch.start()
    val injector = Guice.createInjector(AccioModule, OpsModule, FlagsModule)
    val parserFactory = injector.getInstance(classOf[FlagsParserFactory])
    val parser = parserFactory.create(allowResidue = false, classOf[AccioDocgenFlags])
    parser.parseAndExitUponError(args)

    val flags = try {
      parser.as[AccioDocgenFlags]
    } catch {
      case e: Exception =>
        Console.err.println(s"Error parsing command line: ${e.getMessage.stripPrefix("requirement failed: ")}")
        Console.err.println("Try -help.")
        sys.exit(1)
    }

    val docgen = injector.getInstance(classOf[MarkdownDocgen])
    docgen.generate(flags)
    println(s"Done in ${elapsed().inSeconds} seconds.")
  }
}