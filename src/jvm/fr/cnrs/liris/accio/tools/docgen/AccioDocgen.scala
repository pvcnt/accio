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

package fr.cnrs.liris.accio.tools.docgen

import java.nio.file.Paths

import com.twitter.inject.app.App
import com.twitter.util.Stopwatch
import fr.cnrs.liris.accio.framework.discovery.OpDiscovery
import fr.cnrs.liris.accio.framework.discovery.inject.DiscoveryModule

object AccioDocgenMain extends AccioDocgen

/**
 * Quick and dirty Markdown documentation generator for operators.
 */
class AccioDocgen extends App {
  private[this] val outFlag = flag[String]("out", "File where to write generated documentation")
  private[this] val tocFlag = flag("toc", true, "Whether to include a table of contents")
  private[this] val layoutFlag = flag("layout", "docs", "Layout name")

  override def modules = Seq(DiscoveryModule)

  override def run(): Unit = {
    val elapsed = Stopwatch.start()
    val reader = injector.instance[OpDiscovery]
    val ops = reader.discover.map(reader.read(_).defn)
    println(s"Discovered ${ops.size} operators")
    val docgen = injector.instance[MarkdownDocgen]
    docgen.generate(ops, DocgenOpts(Paths.get(outFlag()), tocFlag(), layoutFlag()))
    println(s"Done in ${elapsed().inSeconds} seconds.")
  }
}