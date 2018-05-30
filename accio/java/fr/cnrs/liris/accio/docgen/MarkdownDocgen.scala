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

package fr.cnrs.liris.accio.docgen

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.file.Files

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.discovery.OpRegistry
import fr.cnrs.liris.accio.domain.Operator

/**
 * Generate documentation for all operators known to a registry in Markdown format.
 */
@Singleton
final class MarkdownDocgen @Inject()(registry: OpRegistry) {
  /**
   * Generate documentation.
   *
   * @param flags Generator options.
   */
  def generate(flags: DocgenOpts): Unit = {
    Files.createDirectories(flags.out)
    val ops = registry.ops
      .groupBy(_.category)
      .toSeq
      .sortBy(_._1)
    ops.zipWithIndex.foreach { case ((category, ops), idx) =>
      val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(flags.out.resolve(s"ops-$category.md").toFile)))
      try {
        writeIntro(out, flags, category, 50 + idx)
        ops.toSeq.sortBy(_.name).foreach(opDef => writeOp(out, opDef))
      } finally {
        out.close()
      }
    }
  }

  private def writeIntro(out: PrintStream, flags: DocgenOpts, category: String, order: Int): Unit = {
    out.println("---")
    out.println(s"""title: "Category: ${category.capitalize}"""")
    out.println(s"order: $order")
    out.println("---\n")

    if (flags.toc) {
      out.println("* TOC")
      out.println("{:toc}\n")
    }
  }

  private def writeOp(out: PrintStream, opDef: Operator): Unit = {
    out.println(s"## ${opDef.name}")
    out.println()
    opDef.deprecation.foreach { deprecation =>
      out.println(s"""<div class="alert alert-warning" markdown="1">**Deprecated:** $deprecation</div>""")
      out.println()
    }
    opDef.description.foreach { description =>
      out.println(description)
      out.println()
    }
    if (opDef.inputs.nonEmpty) {
      out.println("| Input name | Type | Description |")
      out.println("|:-----------|:-----|:------------|")
      opDef.inputs.foreach { argDef =>
        out.print(s"| `${argDef.name}` | ${argDef.dataType.help}")
        if (argDef.defaultValue.isDefined) {
          out.print(s"; optional; default: ${argDef.defaultValue.get.v}")
        } else if (argDef.optional) {
          out.print("; optional")
        } else {
          out.print("; required")
        }
        out.println(s" | ${argDef.help.getOrElse("-")} |")
      }
      out.println("""{: class="table table-striped"}""")
      out.println()
    }
    if (opDef.outputs.nonEmpty) {
      out.println("| Output name | Type | Description |")
      out.println("|:------------|:-----|:------------|")
      opDef.outputs.foreach { argDef =>
        out.println(s"| `${argDef.name}` | ${argDef.dataType.help} | ${argDef.help.getOrElse("-")} |")
      }
      out.println("{: class=\"table table-striped\"}\n")
    }
  }
}