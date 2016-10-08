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

import java.nio.file.{Path, Paths}

import com.google.inject.Inject
import com.twitter.util.Duration
import fr.cnrs.liris.common.util.FileUtils

import scala.reflect.ClassTag

/**
 * A converter is a little helper object that can take a String and turn it into an instance of
 * type T (the type parameter to the converter).
 */
abstract class Converter[T: ClassTag] {
  /**
   * Convert a string into type T.
   */
  @throws[FlagsParsingException]
  def convert(str: String): T

  /**
   * The type description appears in usage messages. E.g.: "a string", "a path", etc.
   */
  def typeDescription: String

  def valueClass: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
}

class StringConverter extends Converter[String] {
  override def convert(str: String): String = str

  override def typeDescription: String = "a string"
}

class ByteConverter extends Converter[Byte] {
  override def convert(str: String): Byte = try {
    str.toByte
  } catch {
    case _: NumberFormatException => throw new FlagsParsingException(s"Invalid byte: $str")
  }

  override def typeDescription: String = "a byte"
}


class ShortConverter extends Converter[Short] {
  override def convert(str: String): Short = try {
    str.toShort
  } catch {
    case _: NumberFormatException => throw new FlagsParsingException(s"Invalid short: $str")
  }

  override def typeDescription: String = "a short integer"
}


class IntConverter extends Converter[Int] {
  override def convert(str: String): Int = try {
    str.toInt
  } catch {
    case _: NumberFormatException => throw new FlagsParsingException(s"Invalid integer: $str")
  }

  override def typeDescription: String = "an integer"
}

class LongConverter extends Converter[Long] {
  override def convert(str: String): Long = try {
    str.toLong
  } catch {
    case _: NumberFormatException => throw new FlagsParsingException(s"Invalid long: $str")
  }

  override def typeDescription: String = "a long"
}

class DoubleConverter extends Converter[Double] {
  override def convert(str: String): Double = try {
    str.toDouble
  } catch {
    case _: NumberFormatException => throw new FlagsParsingException(s"Invalid double: $str")
  }

  override def typeDescription: String = "a double"
}

class PathConverter extends Converter[Path] {
  override def convert(str: String): Path = Paths.get(FileUtils.replaceHome(str))

  override def typeDescription: String = "a path"
}

class DurationConverter extends Converter[Duration] {
  override def convert(str: String): Duration = try {
    Duration.parse(str)
  } catch {
    case _: RuntimeException => throw new FlagsParsingException(s"Invalid duration: $str")
  }

  override def typeDescription: String = "a duration"
}

class BooleanConverter extends Converter[Boolean] {
  override def convert(str: String): Boolean = str.toLowerCase match {
    case "true" | "t" | "1" | "yes" | "y" => true
    case "false" | "f" | "0" | "no" | "n" => false
    case _ => throw new FlagsParsingException(s"Invalid boolean: $str")
  }

  override def typeDescription: String = "a boolean"
}

class TriStateConverter @Inject()(boolConverter: BooleanConverter) extends Converter[TriState] {
  override def convert(str: String): TriState = str.toLowerCase match {
    case "auto" => TriState.Auto
    case _ => if (boolConverter.convert(str)) TriState.Yes else TriState.No
  }

  override def typeDescription: String = "a tri-state (auto, yes, no)"
}