// Large portions of code are copied from Google's Bazel.
/*
 * Copyright 2014 The Bazel Authors. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License
 * for the specific language governing permissions and limitations under the License.
 */

package fr.cnrs.liris.common.flags

import fr.cnrs.liris.common.reflect.CaseClassField

/**
 * The metadata about a flag.
 */
class FlagDescription(val name: String, val defaultValue: Option[_], val converter: Converter[_])

/**
 * The name and value of a flag with additional metadata describing its priority, source, whether
 * it was set via an implicit dependency, and if so, by which other flag.
 *
 * @param name              A flag name
 * @param value             A flag value
 * @param priority          Priority of the thing that set this value for this flag
 * @param source            A thing that set this value for this flag (optional)
 * @param implicitDependant A thing that set this flag by implicit dependency (optional)
 * @param expandedFrom      A thing that set this flag by expansion (optional)
 */
class FlagValueDescription(
    val name: String,
    val value: Any,
    val priority: Priority,
    val source: Option[String],
    val implicitDependant: Option[String],
    expandedFrom: Option[String]) {
  /**
   * Return the thing that set this flag by expansion.
   */
  def expansionParent: String = expandedFrom.get

  /**
   * Check whether this flag is the result of the expansion of another flag.
   */
  def isExpansion: Boolean = expandedFrom.isDefined

  override def toString: String = {
    val sb = new StringBuilder
    sb.append(s"flag '$name' ")
    sb.append(s"set to '$value' ")
    sb.append(s"with priority $priority")
    source.foreach(src => sb.append(s" and source '$src'"))
    implicitDependant.foreach(dep => sb.append(s" implicitly by $dep"))
    sb.toString
  }
}

/**
 * The name and unparsed value of a flag with additional metadata describing its priority,
 * source, whether it was set via an implicit dependency, and if so, by which other flag.
 */
class UnparsedFlagValueDescription(
    val name: String,
    field: CaseClassField,
    val unparsedValue: Option[String],
    val priority: Priority,
    val source: Option[String],
    val explicit: Boolean) {

  def isBooleanOption: Boolean = FlagsParser.isBooleanField(field)

  def isDocumented: Boolean = documentationLevel == DocumentationLevel.Documented

  def isHidden: Boolean = documentationLevel == DocumentationLevel.Hidden

  def isExpansion: Boolean = field.annotation[Flag].expansion.nonEmpty

  def isImplicitRequirement: Boolean = field.annotation[Flag].implicitRequirements.nonEmpty

  override def toString: String = {
    val sb = new StringBuilder
    sb.append("flag '").append(name).append("' ")
    sb.append("set to '").append(unparsedValue).append("' ")
    sb.append("with priority ").append(priority)
    source.foreach(sb.append(" and source '").append(_).append("'"))
    sb.toString
  }

  private def documentationLevel = DocumentationLevel(field.annotation[Flag].category)
}

/**
 * The level of documentation. Only documented flags are output as part of the help.
 *
 * We use 'hidden' so that flags that form the protocol between the client and the server are
 * not logged.
 */
private[flags] sealed trait DocumentationLevel

private[flags] object DocumentationLevel {

  case object Documented extends DocumentationLevel

  case object Undocumented extends DocumentationLevel

  case object Hidden extends DocumentationLevel

  def apply(category: String): DocumentationLevel = category match {
    case "undocumented" => Undocumented
    case "hidden" => Hidden
    case _ => Documented
  }
}
