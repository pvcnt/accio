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

import com.google.common.util.concurrent.UncheckedExecutionException
import fr.cnrs.liris.util.cache.CacheBuilder

import scala.reflect.runtime.{universe => ru}

/**
 * A high-level interface to manipulate case classes with the Scala reflection API.
 *
 * @param fields      List of case fields, in the order specified in the constructor.
 * @param annotations A list of runtime annotations applied on this class.
 * @param scalaType   Type of this class.
 * @param ctor        Constructor reflected method.
 */
final class CaseClass private(
  val annotations: AnnotationList,
  val fields: Seq[CaseClassField],
  val scalaType: ScalaType,
  ctor: ru.MethodMirror) {

  /**
   * Return the associated JVM runtime class.
   */
  def runtimeClass: Class[_] = scalaType.runtimeClass

  /**
   * Create a new instance of this case class, given some arguments. All arguments must be provided,
   * and in the same order as they are defined in `fields`.
   *
   * @param args Values for all fields.
   */
  def newInstance(args: Seq[Any]): Any = {
    require(
      args.lengthCompare(fields.size) == 0,
      s"All arguments should be provided (got ${args.size}, expected ${fields.size})")
    ctor(args: _*)
  }

  override def equals(other: Any): Boolean =
    other match {
      case c: CaseClass => c.scalaType.tpe =:= scalaType.tpe
      case _ => false
    }

  override def hashCode: Int = scalaType.tpe.hashCode
}

object CaseClass {
  private[this] val cache = CacheBuilder().build[String, CaseClass]

  /**
   * Return a struct descriptor for a given case class. Instances are automatically cached, so
   * that there is no penalty of calling that method multiple times with the same type tag.
   *
   * @tparam T Case class type.
   * @throws IllegalArgumentException If `T` is not a case class type, or unsupported.
   */
  def apply[T: ru.TypeTag]: CaseClass = apply(ru.typeOf[T])

  /**
   * Return a struct descriptor for a given case class. Instances are automatically cached, so
   * that there is no penalty of calling that method multiple times with the same type tag.
   *
   * @param tpe Case class type.
   * @throws IllegalArgumentException If `tpe` is not a case class type, or unsupported.
   */
  def apply(tpe: ru.Type): CaseClass =
    try {
      cache(tpe.toString, create(tpe))
    } catch {
      // Unwrap the exception that is otherwise wrapped by Guava into an unchecked exception
      // because, erf, this is Java.
      case e: UncheckedExecutionException => throw e.getCause
    }

  private def create(tpe: ru.Type): CaseClass = {
    if (!tpe.typeSymbol.isClass || !tpe.typeSymbol.asClass.isCaseClass) {
      throw new IllegalArgumentException(s"Not a case class: $tpe")
    }

    // We extract the constructor parameters, which all correspond to the fields of a case class
    // (because all parameters are mapped onto a field).
    val ctorSymbol = tpe.decl(ru.termNames.CONSTRUCTOR).asTerm
    if (ctorSymbol.alternatives.lengthCompare(1) != 0) {
      throw new IllegalArgumentException(s"Case class with multiple constructors is not supported: $tpe")
    }
    // The `typeSignatureIn` allows to "resolve" parametrized types (e.g., `Foo[T]`). We do not
    // want generic parameter types (e.g., `T`) but concrete ones.
    // Because we just checked there is only one constructor, we can take the first parameter list.
    val params = ctorSymbol.typeSignatureIn(tpe).paramLists.head.map(_.asTerm)

    // For everything that concerns annotations, we use those provided by the Java reflection API.
    // With the Scala reflection API, annotations are provided as trees (and not instances), which
    // is really less convenient.
    val clazz = ReflectUtils.classForType(tpe)
    val paramsAnnotations = clazz.getConstructors.head.getParameterAnnotations.toSeq
    if (params.lengthCompare(paramsAnnotations.size) != 0) {
      // We do not support non-static inner classes, because it is too complicated to get the
      // definition of the outer fields. As a side effect, this check will also prevent us from
      // using a class with type bounds, because the constructor will then have more parameters
      // than fields of the case class (because of implicit parameters).
      throw new IllegalArgumentException(s"Non-static inner class is not supported: $tpe")
    }

    // The companion object is used to retrieved parameters' default values. It is guaranteed to
    // exist in the case of a case class.
    val companion = ReflectUtils.reflectInstance(ReflectUtils.reflectCompanion(tpe.typeSymbol.asClass))

    val fields = params.zipWithIndex.map { case (field, idx) =>
      val fieldTpe = field.typeSignature
      new CaseClassField(
        field.name.decodedName.toString.trim,
        idx,
        defaultValue(companion, fieldTpe, idx),
        new AnnotationList(paramsAnnotations(idx)),
        new ScalaType(fieldTpe))
    }

    val classMirror = ReflectUtils.reflectClass(tpe.typeSymbol.asClass)
    val reflectedCtor = classMirror.reflectConstructor(ctorSymbol.asMethod)

    new CaseClass(
      new AnnotationList(clazz.getAnnotations),
      fields,
      new ScalaType(tpe),
      reflectedCtor)
  }

  /**
   * Return a default value for a given field. We attempt to retrieve a default value defined in
   * the constructor, or otherwise fallback to some sensitive defaults for well-known types
   * (e.g., the default value of an [[Option]] is [[None]]).
   *
   * @param refl Instance mirror of the case class companion object
   * @param tpe  Type of the field
   * @param idx  Position of the field in the constructor
   */
  private def defaultValue(refl: ru.InstanceMirror, tpe: ru.Type, idx: Int): Option[_] = {
    val defaultArg = refl.symbol.typeSignature.member(ru.TermName("apply$default$" + (idx + 1)))
    if (defaultArg != ru.NoSymbol) {
      Some(refl.reflectMethod(defaultArg.asMethod).apply())
    } else {
      tpe match {
        case t if t <:< ScalaType.OPTION => Some(None)
        case _ => None
      }
    }
  }
}
