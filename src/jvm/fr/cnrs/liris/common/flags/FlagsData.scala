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

import java.util.concurrent.ConcurrentHashMap

import fr.cnrs.liris.common.reflect.{CaseClass, CaseClassField, JavaTypes}
import fr.cnrs.liris.common.util.StringUtils

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

/**
 * Indicates that a flag is declared in more than one class.
 *
 * @param message Error message.
 */
class DuplicateFlagDeclarationException(message: String) extends RuntimeException(message)

/**
 * An immutable selection of flags data corresponding to a set of flags classes. The data is
 * collected using reflection, which can be expensive. Therefore this class can be used
 * internally to cache the results.
 *
 * @param classes    Flags-declaring classes which are annotated with [[Flag]] annotations.
 * @param fields     Mapping between flag name and Flag-annotated field.
 * @param converters Mapping between Flag-annotated field and the proper converter.
 */
final class FlagsData private(
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
      .map { case (category, categoryFields) =>
        val description = categories.getOrElse(category, s"Flags category '$category'")
        s"\n$description:\n" + categoryFields
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
        sb.append(StringUtils.paragraphFill(annotation.help, indent = 4, width = 80))
        sb.append('\n')
      }
      if (annotation.expansion.nonEmpty) {
        val expandsMsg = "Expands to: " + annotation.expansion.mkString(" ")
        sb.append(StringUtils.paragraphFill(expandsMsg, indent = 4, width = 80))
        sb.append('\n')
      }
    }
    sb.toString
  }

  private def getFlagName(field: CaseClassField) = {
    val name = field.annotation[Flag].name
    if (FlagsParser.isBooleanField(field)) s"[no]$name" else name
  }
}

/**
 * Factory for [[FlagsData]].
 */
object FlagsData {
  private[this] val cache = new ConcurrentHashMap[String, FlagsData].asScala

  /**
   * Return flags data for the given class. Because create it is expensive, result is cached for further calls.
   *
   * @tparam T Flags-declaring type.
   */
  def apply[T: ClassTag]: FlagsData = apply(classTag[T].runtimeClass)

  /**
   * Return flags data for the given classes. Because create it is expensive, result is cached for further calls.
   *
   * @param classes Flags-declaring classes.
   */
  def apply(classes: Class[_]*): FlagsData = {
    val key = classes.map(_.getName).sorted.mkString("|")
    cache.getOrElseUpdate(key, create(classes.toSeq))
  }

  private def create(classes: Seq[Class[_]]): FlagsData = {
    val classesBuilder = mutable.Map.empty[Class[_], CaseClass]
    val fieldsBuilder = mutable.Map.empty[String, CaseClassField]
    val convertersBuilder = mutable.Map.empty[CaseClassField, Converter[_]]

    classes.foreach { clazz =>
      val refl = CaseClass(clazz)
      classesBuilder(refl.runtimeClass) = refl

      refl.fields.foreach { field =>
        require(field.isAnnotated[Flag], "All fields must be annotated with @Flag")

        val flag = field.annotation[Flag]
        require(flag.name.nonEmpty, "Flag cannot have an empty name")

        val maybeDuplicate = fieldsBuilder.put(flag.name, field)
        if (maybeDuplicate.isDefined) {
          throw new DuplicateFlagDeclarationException(
            s"Duplicate flag '${flag.name}', declared in ${maybeDuplicate.get.parentClass.getName} and ${clazz.getName}")
        }
        convertersBuilder(field) = getConverter(field)
      }
    }
    new FlagsData(classesBuilder.toMap, fieldsBuilder.toMap, convertersBuilder.toMap)
  }

  private def getConverter(field: CaseClassField) = {
    val fieldType = if (field.isOption) {
      field.scalaType.typeArguments.head.runtimeClass
    } else {
      field.scalaType.runtimeClass
    }
    val maybeConverter = Converter.get(fieldType)
      .orElse(JavaTypes.maybeAsScala(fieldType).flatMap(Converter.get(_)))
    maybeConverter match {
      case Some(converter) => converter
      case None => throw new RuntimeException(s"No converter registered for ${fieldType.getName}")
    }
  }
}