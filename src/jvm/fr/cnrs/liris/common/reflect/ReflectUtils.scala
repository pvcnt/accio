/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.common.reflect

import scala.reflect.ClassTag
import scala.reflect.api.{TypeCreator, Universe}
import scala.reflect.runtime.universe._

/**
 * Helpers dealing with Scala and Java reflection.
 */
object ReflectUtils {
  val mirror = runtimeMirror(getClass.getClassLoader)

  def cast[T](obj: Any)(implicit tag: TypeTag[T]): T = obj.asInstanceOf[T]

  def runtimeClass[T: ClassTag]: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

  def typeToClass[T](implicit tag: TypeTag[T]): Class[T] = typeToClass(tag.tpe).asInstanceOf[Class[T]]

  def typeToClass(tpe: Type): Class[_] = if (tpe =:= typeOf[Any]) classOf[Any] else mirror.runtimeClass(tpe)

  def typeToClassTag[T](tpe: Type): ClassTag[T] = ClassTag[T](typeToClass(tpe))

  def typeToClassTag[T](tag: TypeTag[T]): ClassTag[T] = ClassTag[T](typeToClass(tag))

  def innerTag[T](tag: TypeTag[T]): TypeTag[_] = innerTag(tag.tpe)

  def innerTag(tpe: Type, idx: Int = 0): TypeTag[_] = ReflectUtils.typeToTypeTag(tpe.typeArgs(idx))

  def innerType[T](tag: TypeTag[T]): Type = innerType(tag.tpe)

  def innerType(tpe: Type, idx: Int = 0): Type = tpe.typeArgs(idx)

  def classToType[T](clazz: Class[T]): Type = mirror.classSymbol(clazz).toType

  def classToTag[T](clazz: Class[T]): TypeTag[T] = typeToTypeTag(classToType(clazz)).asInstanceOf[TypeTag[T]]

  def typeToTypeTag(tpe: Type): TypeTag[_] =
    TypeTag(mirror, new TypeCreator {
      def apply[U <: Universe with Singleton](m: reflect.api.Mirror[U]) = {
        assert(m eq mirror, s"TypeTag[$tpe] defined in $mirror cannot be migrated to $m.")
        tpe.asInstanceOf[U#Type]
      }
    })

  def isAssignableFrom(first: Class[_], second: Class[_]): Boolean = classToType(second) <:< classToType(first)

  def annotation[U <: java.lang.annotation.Annotation : ClassTag](annotations: Seq[java.lang.annotation.Annotation]): U =
    getAnnotation[U](annotations).get

  def getAnnotation[U <: java.lang.annotation.Annotation : ClassTag](annotations: Seq[java.lang.annotation.Annotation]): Option[U] =
    annotations.find(_.annotationType == ReflectUtils.runtimeClass[U]).map(_.asInstanceOf[U])

  def isAnnotated[U <: java.lang.annotation.Annotation : ClassTag](annotations: Seq[java.lang.annotation.Annotation]): Boolean =
    annotations.exists(_.annotationType == ReflectUtils.runtimeClass[U])
}