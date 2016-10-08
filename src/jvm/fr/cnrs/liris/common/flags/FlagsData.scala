// Large portions of code are copied from Google's Bazel.
/*
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

package fr.cnrs.liris.common.flags

import java.text.BreakIterator

import fr.cnrs.liris.common.reflect.{CaseClass, CaseClassField}

/**
 * An immutable selection of flags data corresponding to a set of flags classes. The data is
 * collected using reflection, which can be expensive. Therefore this class can be used
 * internally to cache the results.
 *
 * @param classes    These are the flags-declaring classes which are annotated with [[Flag]] annotations
 * @param fields     Maps flag name to Flag-annotated field
 * @param converters Mapping from each Flag-annotated field to the proper converter
 */
final class FlagsData private[flags](
  val classes: Map[Class[_], CaseClass],
  val fields: Map[String, CaseClassField],
  val converters: Map[CaseClassField, Converter[_]]) {

  /**
   * Returns a description of a set flags.
   *
   * @param verbosity  Verbosity of the description
   * @param categories A mapping from category names to category descriptions. Flags of the same
   *                   category will be grouped together, preceded by the description of the category
   * @see [[HelpVerbosity]]
   */
  def describe(verbosity: HelpVerbosity = HelpVerbosity.Medium, categories: Map[String, String] = Map.empty): String = {
    fields.values.toSeq
      .groupBy(_.annotation[Flag].category)
      .filter { case (category, _) => DocumentationLevel(category) == DocumentationLevel.Documented }
      .map { case (category, fields) =>
        val description = categories.getOrElse(category, s"Flags category '$category'")
        s"\n$description:\n" + fields
          .sortBy(_.annotation[Flag].name)
          .map(describe(_, verbosity).trim)
          .mkString("\n")
      }
      .mkString.trim
  }

  private def describe(field: CaseClassField, verbosity: HelpVerbosity) = {
    val sb = new StringBuilder
    val flagName = getFlagName(field)
    val typeDescription = converters(field).typeDescription
    val annotation = field.annotation[Flag]
    sb.append("  -" + flagName)

    if (verbosity > HelpVerbosity.Short && typeDescription.nonEmpty) {
      sb.append(" (" + typeDescription)
      field.defaultValue
        .filter(v => v != null && v != None) // We do not display uninformative default values
        .foreach(v => sb.append(s"; default: $v"))
      sb.append(")")
    }

    if (verbosity > HelpVerbosity.Medium) {
      sb.append("\n")
      if (annotation.help.nonEmpty) {
        sb.append(paragraphFill(annotation.help, indent = 4, width = 80))
        sb.append('\n')
      }
      if (annotation.expansion.nonEmpty) {
        val expandsMsg = "Expands to: " + annotation.expansion.mkString(" ")
        sb.append(paragraphFill(expandsMsg, indent = 4, width = 80))
        sb.append('\n')
      }
    }
    sb.toString
  }

  private def getFlagName(field: CaseClassField) = {
    val name = field.annotation[Flag].name
    if (FlagsParser.isBooleanField(field)) s"[no]$name" else name
  }

  /**
   * Paragraph-fill the specified input text, indenting lines to 'indent' and
   * wrapping lines at 'width'.  Returns the formatted result.
   */
  private def paragraphFill(in: String, width: Int, indent: Int = 0): String = {
    val indentString = " " * indent
    val out = new StringBuilder
    var sep = ""
    in.split("\n").foreach { paragraph =>
      val boundary = BreakIterator.getLineInstance // (factory)
      boundary.setText(paragraph)
      out.append(sep).append(indentString)
      var cursor = indent
      var start = boundary.first()
      var end = boundary.next()
      while (end != BreakIterator.DONE) {
        val word = paragraph.substring(start, end) // (may include trailing space)
        if (word.length() + cursor > width) {
          out.append('\n').append(indentString)
          cursor = indent
        }
        out.append(word)
        cursor += word.length()
        start = end
        end = boundary.next()
      }
      sep = "\n";
    }
    out.toString
  }
}
