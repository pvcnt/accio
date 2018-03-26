package fr.cnrs.liris.common.reflect

import java.lang.annotation.Annotation

import scala.reflect.{ClassTag, _}

object Annotations {
  def find[T <: Annotation : ClassTag](annotations: Seq[Annotation]): Option[T] =
    annotations.find(_.annotationType == classTag[T].runtimeClass).map(_.asInstanceOf[T])

  def exists[T <: Annotation : ClassTag](annotations: Seq[Annotation]): Boolean =
    annotations.exists(_.annotationType == classTag[T].runtimeClass)
}
