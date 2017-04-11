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

package fr.cnrs.liris.accio.docgen

import java.io.{BufferedOutputStream, FileOutputStream, OutputStream, PrintStream}
import java.nio.file.Files

import com.google.inject.Inject
import fr.cnrs.liris.accio.core.api.OpDef
import fr.cnrs.liris.accio.core.framework.OpRegistry
import fr.cnrs.liris.dal.core.api.{DataTypes, Values}

/**
 * Generate documentation for all operators known to a registry in Markdown format.
 *
 * @param opRegistry Operator registry.
 */
class MarkdownDocgen @Inject()(opRegistry: OpRegistry) {
  /**
   * Generate documentation w.r.t. given options.
   *
   * @param flags Generator options.
   */
  def generate(flags: AccioDocgenFlags): Unit = {
    Files.createDirectories(flags.out)
    opRegistry
      .ops
      .groupBy(_.category)
      .toSeq
      .sortBy(_._1)
      .zipWithIndex
      .foreach { case ((category, ops), idx) =>
        val out = new PrintStream(new BufferedOutputStream(new FileOutputStream(flags.out.resolve(s"ops-$category.md").toFile)))
        try {
          writeIntro(out, flags, category, 50 + idx)
          ops.toSeq.sortBy(_.name).foreach { opMeta =>
            writeOp(out, opMeta)
          }
        } finally {
          out.close()
        }
      }
  }

  private def writeIntro(out: PrintStream, flags: AccioDocgenFlags, category: String, weight: Int) = {
    out.println("---")
    out.println(s"layout: ${flags.layout}")
    out.println(s"""title: "Operators: ${category.capitalize}"""")
    out.println(s"weight: $weight")
    out.println("---\n")

    if (flags.toc) {
      out.println("* TOC")
      out.println("{:toc}\n")
    }
  }

  private def writeOp(out: PrintStream, opDef: OpDef) = {
    out.println(s"## ${opDef.name}")
    out.println()
    opDef.deprecation.foreach { deprecation =>
      out.println(s"""<div class="alert alert-warning" markdown="1"> :broken_heart: **Deprecated:** $deprecation</div>""")
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
        out.print(s"| `${argDef.name}` | ${DataTypes.toString(argDef.kind)}")
        if (argDef.defaultValue.isDefined) {
          out.print(s"; optional; default: ${Values.toString(argDef.defaultValue.get)}")
        } else if (argDef.isOptional) {
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
        out.println(s"| `${argDef.name}` | ${DataTypes.toString(argDef.kind)} | ${argDef.help.getOrElse("-")} |")
      }
      out.println("{: class=\"table table-striped\"}\n")
    }
  }
}