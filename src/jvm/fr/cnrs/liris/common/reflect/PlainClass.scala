package fr.cnrs.liris.common.reflect

import java.lang.annotation.Annotation

import scala.reflect.ClassTag

class PlainClass(val scalaType: ClassType, val annotations: Seq[Annotation]) {
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

object PlainClass {
  def apply(clazz: Class[_]): PlainClass = {
    val typ = ClassType.parse(clazz)
    new PlainClass(typ, clazz.getAnnotations)
  }
}
