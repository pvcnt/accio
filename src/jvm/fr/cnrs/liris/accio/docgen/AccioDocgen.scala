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

    val docgen = injector.getInstance(classOf[DocumentationGenerator])
    docgen.generate(flags)
    println(s"Done in ${elapsed().inSeconds} seconds.")
  }
}

class DocumentationGenerator @Inject()(opRegistry: OpRegistry) {
  def generate(flags: AccioDocgenFlags): Unit = {
    val out = new BufferedOutputStream(new FileOutputStream(flags.out.toFile))
    writeIntro(out, flags)
    opRegistry.ops.groupBy(_.defn.category).foreach { case (category, ops) =>
      out.write(s"## ${category.capitalize} operators\n\n".getBytes)
      ops.toSeq.sortBy(_.defn.name).foreach { opMeta =>
        writeOp(out, opMeta)
      }
    }

    out.close()
  }

  private def writeIntro(out: OutputStream, flags: AccioDocgenFlags) = {
    out.write("---\n".getBytes)
    out.write(s"layout: ${flags.layout}\n".getBytes)
    out.write(s"nav: ${flags.nav}\n".getBytes)
    out.write(s"title: ${flags.title}\n".getBytes)
    out.write("---\n\n".getBytes)

    if (flags.toc) {
      out.write("* TOC\n{:toc}\n\n".getBytes)
    }
  }

  private def writeOp(out: OutputStream, opMeta: OpMeta) = {
    out.write(s"### ${opMeta.defn.name}\n\n".getBytes)
    out.write(s":hammer: Implemented in `${opMeta.opClass.getName}`\n\n".getBytes)
    opMeta.defn.deprecation.foreach { deprecation =>
      out.write(s":broken_heart: **Deprecated:** $deprecation\n\n".getBytes)
    }
    opMeta.defn.help.foreach { help =>
      out.write(s"$help\n\n".getBytes)
    }
    opMeta.defn.description.foreach { description =>
      out.write(s"$description\n\n".getBytes)
    }
    if (opMeta.defn.inputs.nonEmpty) {
      out.write("| Input name | Type | Description |\n".getBytes)
      out.write("|:-----------|:-----|:------------|\n".getBytes)
      opMeta.defn.inputs.foreach { argDef =>
        out.write(s"| `${argDef.name}` | ${argDef.kind.typeDescription}".getBytes)
        if (argDef.defaultValue.isDefined && argDef.defaultValue.get != None) {
          out.write(s"; optional; default: ${argDef.defaultValue.get}".getBytes)
        } else if (argDef.isOptional) {
          out.write("; optional".getBytes)
        }
        out.write(s" | ${argDef.help.getOrElse("-")} |\n".getBytes)
      }
      out.write("{: class=\"table table-striped\"}\n\n".getBytes)
    }
    if (opMeta.defn.outputs.nonEmpty) {
      out.write("| Output name | Type | Description |\n".getBytes)
      out.write("|:------------|:-----|:------------|\n".getBytes)
      opMeta.defn.outputs.foreach { argDef =>
        out.write(s"| `${argDef.name}` | ${argDef.kind.typeDescription} | ${argDef.help.getOrElse("-")} |\n".getBytes)
      }
      out.write("{: class=\"table table-striped\"}\n\n".getBytes)
    }
  }
}