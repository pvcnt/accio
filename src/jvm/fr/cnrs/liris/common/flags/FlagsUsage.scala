package fr.cnrs.liris.common.flags

import fr.cnrs.liris.common.util.TextUtils
import fr.cnrs.liris.common.reflect.ReflectCaseField

object FlagsUsage {
  /**
   * Returns a description of a set flags.
   *
   * @param flagsData  Flags data to describe
   * @param verbosity  Verbosity of the description
   * @param categories A mapping from category names to category descriptions. Flags of the same
   *                   category will be grouped together, preceded by the description of the category
   * @see [[HelpVerbosity]]
   */
  def describe(flagsData: FlagsData, verbosity: HelpVerbosity = HelpVerbosity.Medium, categories: Map[String, String] = Map.empty): String = {
    flagsData.fields.values.toSeq
        .groupBy(_.annotation[Flag].category)
        .filter { case (category, _) => DocumentationLevel(category) == DocumentationLevel.Documented }
        .map { case (category, fields) =>
          val description = categories.getOrElse(category, s"Flags category '$category'")
          s"\n$description:\n" + fields
              .sortBy(_.annotation[Flag].name)
              .map(describe(flagsData, _, verbosity).trim)
              .mkString("\n")
        }
        .mkString.trim
  }

  private def describe(flagsData: FlagsData, field: ReflectCaseField, verbosity: HelpVerbosity) = {
    val sb = new StringBuilder
    val flagName = getFlagName(field)
    val typeDescription = flagsData.converters(field).typeDescription
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
        sb.append(TextUtils.paragraphFill(annotation.help, indent = 4, width = 80))
        sb.append('\n')
      }
      if (annotation.expansion.nonEmpty) {
        val expandsMsg = "Expands to: " + annotation.expansion.mkString(" ")
        sb.append(TextUtils.paragraphFill(expandsMsg, indent = 4, width = 80))
        sb.append('\n')
      }
    }
    sb.toString
  }

  private def getFlagName(field: ReflectCaseField) = {
    val name = field.annotation[Flag].name
    if (FlagsParser.isBooleanField(field)) s"[no]$name" else name
  }
}
