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

package fr.cnrs.liris.accio.tools.docgen

import java.nio.file.Paths

import com.twitter.inject.app.App
import com.twitter.util.Stopwatch
import fr.cnrs.liris.accio.logging.LogbackConfigurator
import fr.cnrs.liris.locapriv.install.OpsModule

object AccioDocgenMain extends AccioDocgen

/**
 * Quick and dirty Markdown documentation generator for operators.
 */
class AccioDocgen extends App with LogbackConfigurator {
  private[this] val outFlag = flag[String]("out", "File where to write generated documentation")
  private[this] val tocFlag = flag("toc", true, "Whether to include a table of contents")

  override def failfastOnFlagsNotParsed = true

  override def run(): Unit = {
    val elapsed = Stopwatch.start()
    val docgen = new MarkdownDocgen
    docgen.generate(DocgenOpts(Paths.get(outFlag()), tocFlag()))
    println(s"Done in ${elapsed().inSeconds} seconds.")
  }
}
