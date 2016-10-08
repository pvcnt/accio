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

package fr.cnrs.liris.common.reflect

import java.lang.annotation.Annotation
import java.util.Objects

import scala.reflect._

class CaseClass(val scalaType: ClassType, val fields: Seq[CaseClassField], val annotations: Seq[Annotation]) {
  def runtimeClass: Class[_] = scalaType.runtimeClass

  def getAnnotation[T <: Annotation : ClassTag]: Option[T] = Annotations.find[T](annotations)

  def annotation[T <: Annotation : ClassTag]: T = Annotations.find[T](annotations).get

  def isAnnotated[T <: Annotation : ClassTag]: Boolean = Annotations.exists[T](annotations)

  override def equals(other: Any): Boolean = other match {
    case c: CaseClass => c.runtimeClass == runtimeClass
    case _ => false
  }

  override def hashCode: Int = runtimeClass.hashCode
}

object CaseClass {
  def apply[T: ClassTag]: CaseClass = apply(classTag[T].runtimeClass)

  def apply(clazz: Class[_]): CaseClass = {
    require(clazz.getConstructors.length == 1, "Multiple constructors case classes are not supported")
    val allAnnotations = clazz.getConstructors.head.getParameterAnnotations.toSeq
    val typ = ClassType.parse(clazz)
    val ctorParams = CaseClassSigParser.parseConstructorParams(clazz)
    require(allAnnotations.size == ctorParams.size, "Non-static inner case classes are not supported")

    val companionObject = Class.forName(clazz.getName + "$").getField("MODULE$").get(null)
    val companionObjectClass = companionObject.getClass

    val fields = ctorParams.zipWithIndex.map { case (ctorParam, idx) =>
      new CaseClassField(
        name = ctorParam.name,
        scalaType = ctorParam.scalaType,
        parentClass = clazz,
        annotations = allAnnotations(idx),
        defaultFuncOpt = defaultFunction(companionObjectClass, companionObject, idx))
    }
    new CaseClass(typ, fields, clazz.getAnnotations)
  }

  private def defaultFunction(companionObjectClass: Class[_], companionObject: Any, ctorParamIdx: Int): Option[() => Any] = {
    val defaultMethodArgNum = ctorParamIdx + 1
    companionObjectClass.getMethods.find { method =>
      method.getName == "$lessinit$greater$default$" + defaultMethodArgNum
    }.map { method =>
      () => method.invoke(companionObject)
    }
  }
}

class CaseClassField(
  val name: String,
  val scalaType: ScalaType,
  val parentClass: Class[_],
  val annotations: Seq[Annotation],
  defaultFuncOpt: Option[() => Any]) {

  val isOption = scalaType.runtimeClass == classOf[Option[_]]

  def defaultValue: Option[Any] = defaultFuncOpt.map(_ ()).orElse(if (isOption) Some(None) else None)

  def getAnnotation[T <: Annotation : ClassTag]: Option[T] = Annotations.find[T](annotations)

  def annotation[T <: Annotation : ClassTag]: T = Annotations.find[T](annotations).get

  def isAnnotated[T <: Annotation : ClassTag]: Boolean = Annotations.exists[T](annotations)

  override def equals(other: Any): Boolean = other match {
    case f: CaseClassField => f.parentClass == parentClass && f.name == name
    case _ => false
  }

  override def hashCode: Int = Objects.hash(parentClass, name)
}
