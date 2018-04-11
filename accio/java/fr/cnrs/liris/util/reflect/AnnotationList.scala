/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
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

package fr.cnrs.liris.util.reflect

import java.lang.annotation.Annotation

import scala.reflect.{ClassTag, classTag}

/**
 * A helper to manage a list of annotations.
 *
 * @param annotations An array of annotations.
 */
final class AnnotationList private[reflect](annotations: Array[Annotation]) {
  /**
   * Return the instance of an annotation of a given type, if any.
   *
   * @tparam T Annotation type.
   */
  def get[T <: Annotation : ClassTag]: Option[T] =
    annotations.find(_.annotationType == classTag[T].runtimeClass).map(_.asInstanceOf[T])

  /**
   * Return whether there is an annotation of a given type.
   *
   * @tparam T Annotation type.
   */
  def contains[T <: Annotation : ClassTag]: Boolean = annotations.exists(_.annotationType == classTag[T].runtimeClass)
}

object AnnotationList {
  /**
   * Return a list of annotations for a given class.
   *
   * @tparam T Class type.
   */
  def of[T: ClassTag]: AnnotationList = of(classTag[T].runtimeClass)

  /**
   * Return a list of annotations for a given class.
   *
   * @param clazz Class.
   */
  def of(clazz: Class[_]): AnnotationList = new AnnotationList(clazz.getAnnotations)
}
