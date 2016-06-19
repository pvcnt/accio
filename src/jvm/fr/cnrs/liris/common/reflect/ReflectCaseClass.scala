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

import java.util.Objects

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A high-level interface to manipulate case classes with the Scala reflection API.
 *
 * @param tpe         Type of this class
 * @param fields      List of case fields, in the order specified in the constructor
 * @param annotations A list of runtime annotations applied on this class
 */
class ReflectCaseClass(val tpe: Type, val annotations: Seq[java.lang.annotation.Annotation], val fields: Seq[ReflectCaseField]) {
  def classTag: ClassTag[_] = ReflectUtils.typeToClassTag(tpe)

  /**
   * Return the Java runtime class corresponding to this class.
   *
   * @return A Java class
   */
  def runtimeClass: Class[_] = ReflectUtils.typeToClass(tpe)

  /**
   * Return an annotation applied on this class of the given type (it must exist to be retrieved).
   *
   * @tparam U Annotation type
   * @return An annotation
   */
  def annotation[U <: java.lang.annotation.Annotation : ClassTag]: U =
    ReflectUtils.annotation[U](annotations)

  /**
   * Check whether this class is annotated with a given annotation.
   *
   * @tparam U Annotation type
   * @return True if there is an annotation of this type applied on this class, false otherwise
   */
  def isAnnotated[U <: java.lang.annotation.Annotation : ClassTag]: Boolean =
    ReflectUtils.isAnnotated[U](annotations)

  def newInstance(values: Seq[Any]): Any = {
    require(values.size == fields.size,
      s"Number of arguments mismatch (got ${values.size}, expected ${fields.size})")
    runtimeClass.getConstructors.head.newInstance(values.map(_.asInstanceOf[AnyRef]): _*)
  }

  override def equals(other: Any): Boolean = other match {
    case c: ReflectCaseClass => c.tpe =:= tpe
    case _ => false
  }

  override def hashCode: Int = tpe.hashCode
}

/**
 * A high-level interface to manipulate case classes fields with the Scala reflection API.
 *
 * @param tpe          Type of this field
 * @param name         Field name
 * @param defaultValue Possibily a default value for this field
 * @param annotations  A list of runtime annotations applied on this field
 */
class ReflectCaseField(
    protected val parentClass: Class[_],
    val tpe: Type,
    val name: String,
    val index: Int,
    val defaultValue: Option[_],
    val annotations: Seq[java.lang.annotation.Annotation]
) {
  def classTag: ClassTag[_] = ReflectUtils.typeToClassTag(tpe)

  def runtimeClass: Class[_] = ReflectUtils.typeToClass(tpe)

  def typeTag: TypeTag[_] = ReflectUtils.typeToTypeTag(tpe)

  /**
   * Return an annotation applied on this field of the given type (it must exist to be retrieved).
   *
   * @tparam U Annotation type
   * @return An annotation
   */
  def annotation[U <: java.lang.annotation.Annotation : ClassTag]: U =
    ReflectUtils.annotation[U](annotations)

  def getAnnotation[U <: java.lang.annotation.Annotation : ClassTag]: Option[U] =
    ReflectUtils.getAnnotation[U](annotations)

  /**
   * Check whether this field is annotated with a given annotation.
   *
   * @tparam U Annotation type
   * @return True if there is an annotation of this type applied on this class, false otherwise
   */
  def isAnnotated[U <: java.lang.annotation.Annotation : ClassTag]: Boolean =
    ReflectUtils.isAnnotated[U](annotations)

  def get(obj: Any): Any = runtimeClass.getDeclaredField(name).get(obj)

  override def equals(other: Any): Boolean = other match {
    case f: ReflectCaseField => f.parentClass == parentClass && f.index == index
    case _ => false
  }

  override def hashCode: Int = Objects.hash(parentClass, index.asInstanceOf[AnyRef])
}

/**
 * Factory for [[ReflectCaseClass]].
 */
object ReflectCaseClass {
  private[this] val cache = mutable.Map.empty[String, ReflectCaseClass]

  def of[T](implicit typeTag: TypeTag[T]): ReflectCaseClass = of(typeTag.tpe)

  /**
   * Return a reflection for a given case class. This only works for case classes. Constructing
   * these instances is expensive, you should avoid requiring them multiple times.
   *
   * @param tpe A case class type
   * @return A case class reflection
   */
  def of(tpe: Type): ReflectCaseClass = cache.getOrElseUpdate(tpe.toString, create(tpe))

  private def create(tpe: Type): ReflectCaseClass = {
    val clazz = ReflectUtils.typeToClass(tpe)

    //Note: for everything that concerns annotations, we use those provided by the Java reflection API. With Scala API,
    //annotations are provided by Scala as trees, and not instances.

    //Then we extract the constructor parameters. We must be careful to always distinguish between parameters and fields
    //which represent the same "thing" but have different properties. E.g., annotations are bounded to the parameters
    //(and not the fields), and type signatures are different (T vs. => T).
    val ctorTerm = tpe.decl(termNames.CONSTRUCTOR).asTerm
    require(ctorTerm.alternatives.size == 1, s"Case class ${clazz.getName} with multiple constructors is not supported")
    val ctor = ctorTerm.asMethod
    val params = ctor.paramLists.head.map(_.asTerm)

    //We extract all fields corresponding to case class parameters.
    val fieldsTerms = tpe.members.sorted.filter(_.isTerm).map(_.asTerm).filter(m => m.isCaseAccessor && !m.isPrivate)
    val paramsAnnotations = clazz.getConstructors.head.getParameterAnnotations.toSeq
    require(params.size == fieldsTerms.size && paramsAnnotations.size == fieldsTerms.size,
      s"Non static inner class ${clazz.getName} is not supported")

    //The case class's companion object is used to retrieved parameters' default values (which are materialized as
    //methods providing the value).
    val classSymb = tpe.typeSymbol.asClass
    val moduleSymb = classSymb.companion.asModule
    val refl = ReflectUtils.mirror.reflect(ReflectUtils.mirror.reflectModule(moduleSymb).instance)

    val fields = fieldsTerms.zipWithIndex.map { case (field, idx) =>
      val annotations = paramsAnnotations(idx).toSeq
      val tpe = params(idx).typeSignature
      val name = field.name.decodedName.toString.trim
      new ReflectCaseField(clazz, tpe, name, idx, defaultValue(refl, tpe, idx), annotations)
    }

    new ReflectCaseClass(tpe, clazz.getAnnotations.toSeq, fields)
  }

  /**
   * Return a default value for a given field. We attempt to retrieve a default value defined in the constructor, or
   * otherwise fallback to some sensitive defaults for well-known types (e.g., Option[_], Iterable[_]).
   *
   * @param refl Instance mirror of the case class companion object
   * @param tpe  Type of the field
   * @param idx  Position of the field in the constructor
   * @return Possibily a default value, None if unable to provide one
   */
  private def defaultValue(refl: InstanceMirror, tpe: Type, idx: Int): Option[_] = {
    val defaultArg = refl.symbol.typeSignature.member(TermName("apply$default$" + (idx + 1)))
    if (defaultArg != NoSymbol) {
      Some(refl.reflectMethod(defaultArg.asMethod).apply())
    } else {
      tpe match {
        case t if t <:< typeOf[Iterable[_]] => Some(Iterable.empty)
        case t if t <:< typeOf[Option[_]] => Some(None)
        case _ => None
      }
    }
  }
}