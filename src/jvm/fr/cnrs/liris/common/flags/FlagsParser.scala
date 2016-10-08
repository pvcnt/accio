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

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * An exception that's thrown when the [[FlagsParser]] fails.
 *
 * @param message         Error message
 * @param invalidArgument Name of the invalid argument
 * @param cause           Cause
 */
class FlagsParsingException(message: String, val invalidArgument: Option[String] = None, cause: Throwable = null) extends Exception(message, cause)

/**
 * A parser for flags. Typical use case in a main method:
 *
 * {{{
 * val parser = FlagsParser(allowResidue = true, typeOf[FooFlags],typeOf[BarFlags])
 * parser.parseAndExitUponError(args)
 * val foo = parser.as[FooFlags]
 * val bar = parser.as[BarFlags]
 * val otherArguments = parser.residue
 * }}}
 *
 * FooFlags and BarFlags would be flags specification case classes whose all constructor fields
 * are annotated with @Flag(...).
 *
 * Alternatively, rather than calling [[FlagsParser.parseAndExitUponError()]], client code may
 * call [[FlagsParser.parse()]], and handle parser exceptions usage messages themselves.
 *
 * This flags parsing implementation has (at least) one design flaw. It allows both '--foo=baz'
 * and '--foo baz' for all flags except boolean and tristate flags. For these, the 'baz'
 * in '--foo baz' is not treated as a parameter to the flag, making it is impossible to switch
 * flags between void/boolean/tristate and everything else without breaking backwards compatibility.
 */
class FlagsParser(flagsData: FlagsData, allowResidue: Boolean) extends FlagsProvider {
  private val _residue = mutable.ListBuffer.empty[String]
  private val impl = new IncrementalFlagsParser(flagsData)

  /**
   * Parses `args`, using the classes registered with this parser.
   *
   * May be called multiple times; later flags override existing ones if they have equal or
   * higher priority. The source of flags is a free-form string that can be used for debugging.
   * Strings that cannot be parsed as flags accumulates as residue, if this parser allows it.
   */
  @throws[FlagsParsingException]
  def parse(args: Seq[String], priority: Priority = Priority.CommandLine, source: Option[String] = None): Unit = {
    require(priority != Priority.Default)
    _residue ++= impl.parse(priority, source, None, None, args)
    if (!allowResidue && _residue.nonEmpty) {
      throw new FlagsParsingException(s"Unrecognized arguments: ${residue.mkString(" ")}")
    }
  }

  /**
   * A convenience function for use in main methods. If "-help" appears anywhere within `args`,
   * prints out the usage message without parsing anything. Otherwise parse the command line
   * parameters, and exit upon error.
   */
  def parseAndExitUponError(args: Seq[String], priority: Priority = Priority.CommandLine, source: Option[String] = None): Unit = {
    if (args.contains("-help")) {
      println(describeFlags(HelpVerbosity.Long))
      sys.exit(0)
    } else {
      try {
        parse(args, priority, source)
      } catch {
        case e: FlagsParsingException =>
          Console.err.println(s"Error parsing command line: ${e.getMessage}")
          Console.err.println("Try -help.")
          sys.exit(1)
      }
    }
  }

  /**
   * Return a description of the flag value set by the last previous call to [[parse()]] that
   * successfully set the given flag, if there is a value for it.
   *
   * @throws IllegalArgumentException if there is no flag with the given name
   */
  @throws[IllegalArgumentException]
  def getFlagValueDescription(name: String): Option[FlagValueDescription] = {
    val field = flagsData.fields.get(name)
    require(field.isDefined, s"No such flag '$name'")
    impl.parsedValues.get(field.get).map(_.asFlagValueDescription(name))
  }

  /**
   * Return a description of the flag, if there is a flag for the given name.
   */
  def getFlagDescription(name: String): Option[FlagDescription] =
  flagsData.fields.get(name).map { field =>
    new FlagDescription(name, field.defaultValue, flagsData.converters(field))
  }

  /**
   * Returns a description of all the flags this parser can digest.
   *
   * @param verbosity  Verbosity of the description
   * @param categories A mapping from category names to category descriptions
   * @see [[FlagsData.describe()]]
   */
  def describeFlags(verbosity: HelpVerbosity, categories: Map[String, String] = Map.empty): String =
  flagsData.describe(verbosity, categories)

  /**
   * Return a list of new warnings that were generated by calls to [[parse()]].
   */
  def warnings: Seq[String] = impl.warnings

  override def residue: Seq[String] = _residue.toList

  override def as[T: ClassTag]: T = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    require(flagsData.classes.contains(clazz), s"Flag class unknown to this parser: ${clazz.getName}")

    val ctor = clazz.getConstructors.head
    val args = flagsData.classes(clazz).fields.map { field =>
      val value = getValue(field).map { value =>
        if (field.isOption) Some(value) else value
      }.orElse(field.defaultValue)
      require(value.isDefined, s"No value for flag '${field.annotation[Flag].name}'")
      value.get
    }
    ctor.newInstance(args.map(_.asInstanceOf[AnyRef]): _*).asInstanceOf[T]
  }

  override def asListOfUnparsedFlags: Seq[UnparsedFlagValueDescription] = {
    // It is vital that this sort is stable so that flags on the same priority are not reordered.
    impl.unparsedValues.sortBy(_.priority).toList
  }

  override def asListOfExplicitFlags: Seq[UnparsedFlagValueDescription] = {
    // It is vital that this sort is stable so that flags on the same priority are not reordered.
    impl.unparsedValues.filter(_.explicit).sortBy(_.priority).toList
  }

  override def canonicalize: Seq[String] =
    impl.canonicalValues
      .sortWith { (o1, o2) =>
        // Sort implicit requirement flags to the end, keeping their existing order, and sort
        // the other options alphabetically.
        if (o1.isImplicitRequirement) {
          false
        } else if (o2.isImplicitRequirement) {
          true
        } else {
          o1.name.compareTo(o2.name) < 0
        }
      }
      .filterNot(_.isExpansion) // Ignore expansion flags
      .map(v => s"-${v.name}=${v.unparsedValue}")

  override def asListOfEffectiveFlags: Seq[FlagValueDescription] =
    flagsData.fields.map { case (name, field) =>
      impl.parsedValues.get(field)
        .map(_.asFlagValueDescription(name))
        .getOrElse(new FlagValueDescription(name, field.defaultValue, Priority.Default, None, None, None))
    }.toSeq

  override def containsExplicitFlag(name: String): Boolean = {
    val field = flagsData.fields.get(name)
    require(field.isDefined, s"No such flag '$name'")
    impl.parsedValues.contains(field.get)
  }

  private def getValue(field: CaseClassField) = impl.parsedValues.get(field).map(_.value)
}

object FlagsParser {
  private[flags] def isBooleanField(field: CaseClassField) =
    field.scalaType.runtimeClass == classOf[Boolean] ||
      field.scalaType.runtimeClass == classOf[java.lang.Boolean] ||
      field.scalaType.runtimeClass == classOf[TriState]
}
