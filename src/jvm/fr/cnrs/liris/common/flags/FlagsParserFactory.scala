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

import java.util.concurrent.ConcurrentHashMap

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.common.reflect.{CaseClass, CaseClassField, JavaTypes}

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.reflect._

/**
 * Indicates that a flag is declared in more than one class.
 */
class DuplicateFlagDeclarationException(message: String) extends RuntimeException(message)

@Singleton
class FlagsParserFactory @Inject()(converters: immutable.Set[Converter[_]]) {
  private[this] val convertersMap: Map[Class[_], Converter[_]] = converters.map(converter => converter.valueClass -> converter).toMap
  private[this] val cache = new ConcurrentHashMap[String, FlagsData].asScala

  def create[T: ClassTag]: FlagsParser = create(classTag[T].runtimeClass)

  def create(classes: Class[_]*): FlagsParser = create(allowResidue = true, classes: _*)

  def create(allowResidue: Boolean, classes: Class[_]*): FlagsParser = {
    val key = classes.map(_.getName).sorted.mkString("|")
    val flagsData = cache.getOrElseUpdate(key, createFlagsData(classes.toSeq))
    new FlagsParser(flagsData, allowResidue)
  }

  private def createFlagsData(create: Seq[Class[_]]): FlagsData = {
    val classesBuilder = mutable.Map.empty[Class[_], CaseClass]
    val fieldsBuilder = mutable.Map.empty[String, CaseClassField]
    val convertersBuilder = mutable.Map.empty[CaseClassField, Converter[_]]

    create.foreach { clazz =>
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
    val maybeConverter = convertersMap.get(fieldType)
      .orElse(JavaTypes.maybeAsScala(fieldType).flatMap(convertersMap.get))
    maybeConverter match {
      case Some(converter) => converter
      case None => throw new RuntimeException(s"No converter registered for ${fieldType.getName}")
    }
  }
}
